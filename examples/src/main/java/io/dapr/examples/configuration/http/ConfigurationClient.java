/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.examples.configuration.http;

import io.dapr.client.DaprApiProtocol;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.*;
import io.dapr.config.Properties;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigurationClient {

  private static final String CONFIG_STORE_NAME = "configstore";
  private static final String APP_ID = "subscriber";

  /**
   * Executes various methods to check the different apis.
   * @param args arguments
   * @throws Exception throws Exception
   */
  public static void main(String[] args) throws Exception {
    System.getProperties().setProperty(Properties.API_PROTOCOL.getName(), DaprApiProtocol.HTTP.name());
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      subscribeConfigurationRequest(client);
      unsubscribeConfigurationItems(client);
    }
  }

  /**
   * Subscribe to a list of keys.Optional to above iterator way of retrieving the changes
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequest(DaprPreviewClient client) {
    System.out.println("Subscribing to key: myconfig1");
    AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>();
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(
        CONFIG_STORE_NAME, Collections.singletonList("myconfig1"));
    Runnable subscribeTask = () -> {
      Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(req);
      disposableAtomicReference.set(outFlux
          .subscribe(
              cis -> {
                cis.getItems().forEach(ConfigurationClient::print);
              }
          ));
    };
    new Thread(subscribeTask).start();

    // To ensure that subscribeThread gets scheduled
    inducingSleepTime(0);

    Runnable updateKeys = () -> {
      int i = 1;
      while (i <= 3) {
        executeDockerCommand("myconfig1", i);
        i++;
      }
    };
    new Thread(updateKeys).start();

    // To ensure main thread does not die before outFlux subscribe gets called
    inducingSleepTime(5000);
  }

  /**
   * Unsubscribe using subscription id.
   *
   * @param client DaprPreviewClient object
   */
  public static void unsubscribeConfigurationItems(DaprPreviewClient client) {
    System.out.println("Subscribing to key: myconfig2");
    AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>();
    AtomicReference<String> subscriptionId = new AtomicReference<>();
    Runnable subscribeTask = () -> {
      Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(CONFIG_STORE_NAME, "myconfig2");
      disposableAtomicReference.set(outFlux
          .subscribe(cis -> {
                subscriptionId.set(cis.getSubscriptionId());
                cis.getItems().forEach(ConfigurationClient::print);
              }
          ));
    };
    new Thread(subscribeTask).start();

    // To ensure that subscribeThread gets scheduled
    inducingSleepTime(0);

    Runnable updateKeys = () -> {
      int i = 1;
      while (i <= 5) {
        executeDockerCommand("myconfig2", i);
        i++;
      }
    };
    new Thread(updateKeys).start();

    // To ensure key starts getting updated
    inducingSleepTime(1000);

    UnsubscribeConfigurationResponse res = client.unsubscribeConfiguration(
        subscriptionId.get(),
        CONFIG_STORE_NAME
    ).block();

    if (res != null) {
      System.out.println("Is Unsubscribe successful: " + res.getIsUnsubscribed());
    } else {
      System.out.println("Unsubscribe unsuccessful!!");
    }
  }

  private static void inducingSleepTime(int timeInMillis) {
    try {
      Thread.sleep(timeInMillis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void print(ConfigurationItem item) {
    System.out.println(item.getValue() + " : key ->" + item.getKey());
  }

  private static void executeDockerCommand(String key, int postfix) {
    String[] command = new String[] {
        "docker", "exec", "dapr_redis", "redis-cli",
        "SET",
        key, "update_myconfigvalue" + postfix + "||2"
    };
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    Process process = null;
    try {
      process = processBuilder.start();
      process.waitFor();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
