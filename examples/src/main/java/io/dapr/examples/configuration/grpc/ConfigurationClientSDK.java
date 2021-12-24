package io.dapr.examples.configuration.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetBulkConfigurationRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationClientSDK {
  private static final String configurationStoreName = "redisconfigstore";
  private static String key = "configgrpc||myconfig";

  public static void main(String[] args) throws Exception {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      usingConfigurationrequest(client);
      usingParameters(client);
      usingVarargs(client);
      usingBulkCofigreq(client);
      getAllUsingCofigreq(client);
      getAllUsingVarargs(client);
      errorUsingParameters(client);
    }
  }

  public static void usingConfigurationrequest(DaprClient client) {
    System.out.println("trying to fetch using Configuration request...");
    GetConfigurationRequest request = new GetConfigurationRequest(configurationStoreName, key);
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(request);
      System.out.println("Retrieved value from configurationstore, Value -> "+ item.block().getValue() + " : key ->" + item.block().getKey());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void usingParameters(DaprClient client) {
    System.out.println("trying to fetch using Configuration parameters...");
    key = "myconfig2";
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(configurationStoreName, key);
      System.out.println("Retrieved value from configurationstore, Value -> "+ item.block().getValue() + " : key ->" + item.block().getKey());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void usingVarargs(DaprClient client) {
    System.out.println("trying to fetch using Varargs...");
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(configurationStoreName, "configgrpc||myconfig", "myconfig2", "myconfig3");
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void usingBulkCofigreq(DaprClient client) {
    System.out.println("trying to fetch using usingBulkCofigreq...");
    List<String> keys = new ArrayList<>();
    keys.add("myconfig2");
    keys.add("myconfig3");
    keys.add("myconfig4");
    keys.add("myconfig5");
    GetBulkConfigurationRequest req = new GetBulkConfigurationRequest(configurationStoreName, keys);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(req);
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void getAllUsingCofigreq(DaprClient client) {
    System.out.println("trying to fetch using getAllUsingCofigreq...");
    GetBulkConfigurationRequest req = new GetBulkConfigurationRequest(configurationStoreName, null);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(req);
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void getAllUsingVarargs(DaprClient client) {
    System.out.println("trying to fetch using getAllUsingVarargs...");
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(configurationStoreName);
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void errorUsingParameters(DaprClient client) {
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(configurationStoreName, "");
      System.out.println("Retrieved value from configurationstore, Value -> "+ item.block().getValue() + " : key ->" + item.block().getKey());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  private static void print(ConfigurationItem item) {
    System.out.println(item.getValue() + " : key ->" + item.getKey());
  }
}
