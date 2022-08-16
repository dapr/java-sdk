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
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.config.Properties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      getConfigurations(client);
      subscribeConfigurationRequest(client);
      unsubscribeConfigurationItems(client);
    }
  }

  /**
   * Gets configurations for a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurations(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configurations for a list of keys********");
    List<String> keys = new ArrayList<>();
    keys.add("myconfig1");
    keys.add("myconfig2");
    keys.add("myconfig3");
    GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, keys);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfiguration(req);
      items.block().forEach(ConfigurationClient::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  /**
   * Subscribe to a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequest(DaprPreviewClient client) {
    System.out.println("Subscribing to key: myconfig2");
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(
        CONFIG_STORE_NAME, Collections.singletonList("myconfig2"));
    Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(req);
    outFlux.subscribe(
        cis -> {
          SUBSCRIPTION_ID = cis.getSubscriptionId();
        });
    System.out.println("Getting updated values for all subscribed keys..");
    int i = 1;
    while (i <= 3) {
      executeDockerCommand("myconfig2", i);
      i++;
    }
  }

  /**
   * Unsubscribe API.
   *
   * @param client DaprPreviewClient object
   */
  public static void unsubscribeConfigurationItems(DaprPreviewClient client) {
    System.out.println("Unsubscribing to key: myconfig2");
    UnsubscribeConfigurationResponse res = client.unsubscribeConfiguration(
            SUBSCRIPTION_ID,
            CONFIG_STORE_NAME
    ).block();

    if (res != null) {
      System.out.println("Is Unsubscribe successful: " + res.getIsUnsubscribed());
    } else {
      System.out.println("Unsubscribe unsuccessful!!");
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
