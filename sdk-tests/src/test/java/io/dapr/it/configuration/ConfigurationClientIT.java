/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.configuration;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("testcontainers")
public class ConfigurationClientIT {

  private static final String CONFIG_STORE_NAME = "redisconfigstore";

  private static final Network NETWORK = Network.newNetwork();

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withNetwork(NETWORK)
      .withNetworkAliases("redis");

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(NETWORK)
      .withAppName("configuration-it")
      .withComponent(new Component(
          CONFIG_STORE_NAME,
          "configuration.redis",
          "v1",
          Map.of("redisHost", "redis:6379", "redisPassword", "")));

  @BeforeAll
  public static void init() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      daprClient.waitForSidecar(10000).block();
    }
  }

  @BeforeEach
  public void setupConfigStore() {
    executeRedisCommand(
        "MSET",
        "myconfigkey1", "myconfigvalue1||1",
        "myconfigkey2", "myconfigvalue2||1",
        "myconfigkey3", "myconfigvalue3||1");
  }

  @Test
  public void getConfiguration() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      ConfigurationItem ci = daprClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1").block();
      assertEquals("myconfigvalue1", ci.getValue());
    }
  }

  @Test
  public void getConfigurations() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      Map<String, ConfigurationItem> cis = daprClient
          .getConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2").block();
      assertEquals(2, cis.size());
      assertTrue(cis.containsKey("myconfigkey1"));
      assertTrue(cis.containsKey("myconfigkey2"));
      assertEquals("myconfigvalue2", cis.get("myconfigkey2").getValue());
    }
  }

  @Test
  public void subscribeConfiguration() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      Runnable subscribeTask = () -> {
        Flux<SubscribeConfigurationResponse> outFlux = daprClient
            .subscribeConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2");
        outFlux.subscribe(update -> {
          if (update.getItems().size() == 0) {
            assertTrue(update.getSubscriptionId().length() > 0);
          } else {
            String value = update.getItems().entrySet().stream().findFirst().get().getValue().getValue();
            assertEquals(1, update.getItems().size());
            assertTrue(value.contains("update_"));
          }
        });
      };
      Thread subscribeThread = new Thread(subscribeTask);
      subscribeThread.start();

      inducingSleepTime(0);
      executeRedisCommand(
          "MSET",
          "myconfigkey1", "update_myconfigvalue1||2",
          "myconfigkey2", "update_myconfigvalue2||2",
          "myconfigkey3", "update_myconfigvalue3||2");

      inducingSleepTime(5000);
    }
  }

  @Test
  public void unsubscribeConfigurationItems() throws Exception {
    try (DaprClient daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      List<String> updatedValues = new ArrayList<>();
      AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>();
      AtomicReference<String> subscriptionId = new AtomicReference<>();
      Runnable subscribeTask = () -> {
        Flux<SubscribeConfigurationResponse> outFlux = daprClient
            .subscribeConfiguration(CONFIG_STORE_NAME, "myconfigkey1");
        disposableAtomicReference.set(outFlux
            .subscribe(update -> {
                  subscriptionId.set(update.getSubscriptionId());
                  updatedValues.add(update.getItems().entrySet().stream().findFirst().get().getValue().getValue());
                }
            ));
      };
      new Thread(subscribeTask).start();

      inducingSleepTime(0);
      Runnable updateKeys = () -> {
        int i = 1;
        while (i <= 5) {
          executeRedisCommand("SET", "myconfigkey1", "update_myconfigvalue" + i + "||2");
          i++;
        }
      };
      new Thread(updateKeys).start();

      inducingSleepTime(1000);

      UnsubscribeConfigurationResponse res = daprClient.unsubscribeConfiguration(
          subscriptionId.get(),
          CONFIG_STORE_NAME
      ).block();

      assertTrue(res != null);
      assertTrue(res.getIsUnsubscribed());
      int listSize = updatedValues.size();

      inducingSleepTime(1000);
      new Thread(updateKeys).start();
      inducingSleepTime(2000);
      assertEquals(listSize, updatedValues.size());

      Disposable disposable = disposableAtomicReference.get();
      if (disposable != null && !disposable.isDisposed()) {
        disposable.dispose();
      }
    }
  }

  private static void inducingSleepTime(int timeInMillis) {
    try {
      Thread.sleep(timeInMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private static void executeRedisCommand(String... args) {
    try {
      var command = new String[args.length + 1];
      command[0] = "redis-cli";
      System.arraycopy(args, 0, command, 1, args.length);
      var result = REDIS.execInContainer(command);
      if (result.getExitCode() != 0) {
        throw new RuntimeException("Not zero exit code for Redis command: " + result.getExitCode());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to execute Redis command", e);
    }
  }
}
