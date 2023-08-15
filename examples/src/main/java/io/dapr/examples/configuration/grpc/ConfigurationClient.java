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

package io.dapr.examples.configuration.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigurationClient {

  private static final String CONFIG_STORE_NAME = "configstore";

  private static final List<String> keys = new ArrayList<>(Arrays.asList("myconfig1", "myconfig3", "myconfig2"));

  /**
   * Executes various methods to check the different apis.
   * @param args arguments
   * @throws Exception throws Exception
   */
  public static void main(String[] args) throws Exception {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      System.out.println("Using Dapr client...");
      getConfigurations(client);
      subscribeConfigurationRequest(client);
    }
  }

  /**
   * Gets configurations for a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurations(DaprClient client) {
    System.out.println("*******trying to retrieve configurations for a list of keys********");
    List<String> keys = new ArrayList<>();
    keys.add("myconfig1");
    keys.add("myconfig2");
    keys.add("myconfig3");
    GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, keys);
    try {
      Mono<Map<String, ConfigurationItem>> items = client.getConfiguration(req);
      items.block().forEach((k,v) -> print(v, k));
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  /**
   * Subscribe to a list of keys.Optional to above iterator way of retrieving the changes
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequest(DaprClient client) {
    System.out.println("Subscribing to key: myconfig1");
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(
        CONFIG_STORE_NAME, Collections.singletonList("myconfig1"));
    Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(req);
    Runnable subscribeTask = () -> {
      outFlux.subscribe(cis -> {
        System.out.println("subscription ID : " + cis.getSubscriptionId());
        System.out.println("subscribing to key myconfig1 is successful");
      });
    };
    new Thread(subscribeTask).start();
    // To ensure main thread does not die before outFlux subscribe gets called
    inducingSleepTime(5000);
  }

  private static void inducingSleepTime(int timeInMillis) {
    try {
      Thread.sleep(timeInMillis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void print(ConfigurationItem item, String key) {
    System.out.println(item.getValue() + " : key ->" + key);
  }
}
