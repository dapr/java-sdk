/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.examples.configuration.grpc;

import io.dapr.client.DaprPreviewClient;
import io.dapr.client.DaprPreviewClientBuilder;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetBulkConfigurationRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ConfigurationClient {

  private static final String CONFIG_STORE_NAME = "redisconfigstore";

  private static final List<String> keys = new ArrayList<>(Arrays.asList("myconfig2", "myconfig3", "myconfig4"));

  /**
   * Executes various methods to check the different apis.
   * @param args arguments
   * @throws Exception throws Exception
   */
  public static void main(String[] args) throws Exception {
    try (DaprPreviewClient client = (new DaprPreviewClientBuilder()).build()) {
      System.out.println("Using preview client...");
      getConfigurationForaSingleKey(client);
      getConfigurationsUsingVarargs(client);
      getConfigurations(client);
      getAllConfigurations(client);
      subscribeConfigurationRequestWithIterator(client);
      subscribeConfigurationRequestWithSubscribe(client);
    }
  }

  /**
   * Gets configuration for a single key.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurationForaSingleKey(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configuration a key********");
    GetConfigurationRequest request = new GetConfigurationRequest(CONFIG_STORE_NAME, keys.get(0));
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(request);
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
          client.getConfigurations(CONFIG_STORE_NAME, "configgrpc||myconfig", "myconfig2", "myconfig3");
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
    keys.add("myconfig2");
    keys.add("myconfig3");
    keys.add("myconfig4");
    keys.add("myconfig5");
    GetBulkConfigurationRequest req = new GetBulkConfigurationRequest(CONFIG_STORE_NAME, keys);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(req);
      items.block().forEach(ConfigurationClient::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  /**
   * Gets all configurations.
   *
   * @param client DaprPreviewClient object
   */
  public static void getAllConfigurations(DaprPreviewClient client) {
    System.out.println("*****Retrieving all configurations*******");
    try {
      Mono<List<ConfigurationItem>> items = client.getAllConfigurations(CONFIG_STORE_NAME);
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
  public static void subscribeConfigurationRequestWithIterator(DaprPreviewClient client) {
    System.out.println("*****Subscribing to keys: " + keys.toString() + " *****");
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(CONFIG_STORE_NAME, keys);
    Iterator<List<ConfigurationItem>> itr = client.subscribeToConfigurations(req).toIterable().iterator();
    while (itr.hasNext()) {
      List<ConfigurationItem> cis = itr.next();
      cis.forEach(ConfigurationClient::print);
    }
  }

  /**
   * Subscribe to a list of keys.Optional to above iterator way of retrieving the changes
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequestWithSubscribe(DaprPreviewClient client) {
    System.out.println("*****Subscribing to keys using subscribe method: " + keys.toString() + " *****");
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(CONFIG_STORE_NAME, keys);
    client.subscribeToConfigurations(req)
        .subscribe(
            cis -> cis.forEach(ConfigurationClient::print)
        );
    // Below lines need to be uncommented to stop client from getting killed.
    /*while (true) {}*/
  }

  private static void print(ConfigurationItem item) {
    System.out.println(item.getValue() + " : key ->" + item.getKey());
  }
}
