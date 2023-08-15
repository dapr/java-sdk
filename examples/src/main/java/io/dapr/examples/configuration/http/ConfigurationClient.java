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
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.config.Properties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationClient {

  private static final String CONFIG_STORE_NAME = "configstore";
  private static String SUBSCRIPTION_ID;

  /**
   * Executes various methods to check the different apis.
   * @param args arguments
   * @throws Exception throws Exception
   */
  public static void main(String[] args) throws Exception {
    System.getProperties().setProperty(Properties.API_PROTOCOL.getName(), DaprApiProtocol.HTTP.name());
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

    Map<String, String> hmap = new HashMap<>();
    hmap.put("meta_key","meta_value");
    GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, keys);
    req.setMetadata(hmap);

    try {
      Mono<Map<String, ConfigurationItem>> items = client.getConfiguration(req);
      items.block().forEach((k,v) -> print(v, k));
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  /**
   * Subscribe to a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequest(DaprClient client) throws InterruptedException {
    System.out.println("Subscribing to key: myconfig2");
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(
        CONFIG_STORE_NAME, Collections.singletonList("myconfig2"));
    Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(req);
    outFlux.subscribe(
        cis -> {
          SUBSCRIPTION_ID = cis.getSubscriptionId();
        });
    if (!SUBSCRIPTION_ID.isEmpty()) {
      System.out.println("subscribing to myconfig2 is successful");
    } else {
      System.out.println("error in subscribing to myconfig2");
    }
    Thread.sleep(5000);
  }

  private static void print(ConfigurationItem item, String key) {
    System.out.println(item.getValue() + " : key ->" + key);
  }
}
