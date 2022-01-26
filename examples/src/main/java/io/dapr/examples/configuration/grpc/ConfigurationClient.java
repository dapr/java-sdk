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

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      getConfigurationForaSingleKey(client);
      getConfigurationsUsingVarargs(client);
      getConfigurations(client);
      subscribeConfigurationRequestWithSubscribe(client);
    }
  }

  /**
   * Gets configuration for a single key.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurationForaSingleKey(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configuration given a single key********");
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(CONFIG_STORE_NAME, keys.get(0));
      System.out.println("Value ->" + item.block().getValue() + " key ->" + item.block().getKey());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  /**
   * Gets configurations for varibale no. of arguments.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurationsUsingVarargs(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configurations for a variable no. of keys********");
    try {
      Mono<List<ConfigurationItem>> items =
          client.getConfiguration(CONFIG_STORE_NAME, "myconfig1", "myconfig3");
      items.block().forEach(ConfigurationClient::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
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
   * Subscribe to a list of keys.Optional to above iterator way of retrieving the changes
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequestWithSubscribe(DaprPreviewClient client) {
    System.out.println("*****Subscribing to keys using subscribe method: " + keys.toString() + " *****");
    AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>();
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(CONFIG_STORE_NAME, keys);
    Runnable subscribeTask = () -> {
      Flux<List<ConfigurationItem>> outFlux = client.subscribeToConfiguration(req);
      disposableAtomicReference.set(outFlux
          .subscribe(
              cis -> cis.forEach(ConfigurationClient::print)
          ));
    };
    new Thread(subscribeTask).start();
    try {
      // To ensure that subscribeThread gets scheduled
      Thread.sleep(0);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Runnable updateKeys = () -> {
      int i = 1;
      while (i <= 3) {
        executeDockerCommand(i);
        i++;
      }
    };
    new Thread(updateKeys).start();
    try {
      // To ensure main thread does not die before outFlux subscribe gets called
      Thread.sleep(10000);
      disposableAtomicReference.get().dispose();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void print(ConfigurationItem item) {
    System.out.println(item.getValue() + " : key ->" + item.getKey());
  }

  private static void executeDockerCommand(int postfix) {
    String[] command = new String[] {
        "docker", "exec", "dapr_redis", "redis-cli",
        "SET",
        "myconfig" + postfix, "update_myconfigvalue" + postfix + "||2"
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
