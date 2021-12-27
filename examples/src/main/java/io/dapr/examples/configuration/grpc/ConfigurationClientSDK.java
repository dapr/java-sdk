package io.dapr.examples.configuration.grpc;

import io.dapr.client.DaprPreviewClient;
import io.dapr.client.DaprPreviewClientBuilder;
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
    try (DaprPreviewClient client = (new DaprPreviewClientBuilder()).build()) {
      System.out.println("Using preview client...");
      usingConfigurationrequest(client);
      usingParameters(client);
      usingVarargs(client);
      usingBulkCofigreq(client);
      getAllUsingCofigreq(client);
      getAllUsingVarargs(client);
      errorUsingParameters(client);
    }
  }

  public static void usingConfigurationrequest(DaprPreviewClient client) {
    System.out.println("trying to fetch using Configuration request...");
    GetConfigurationRequest request = new GetConfigurationRequest(configurationStoreName, key);
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(request);
      System.out.println("Retrieved value from configurationstore, Value -> "+ item.block().getValue() + " : key ->" + item.block().getKey());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void usingParameters(DaprPreviewClient client) {
    System.out.println("trying to fetch using Configuration parameters...");
    key = "myconfig2";
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(configurationStoreName, key);
      System.out.println("Retrieved value from configurationstore, Value -> "+ item.block().getValue() + " : key ->" + item.block().getKey());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void usingVarargs(DaprPreviewClient client) {
    System.out.println("trying to fetch using Varargs...");
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(configurationStoreName, "configgrpc||myconfig", "myconfig2", "myconfig3");
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void usingBulkCofigreq(DaprPreviewClient client) {
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

  public static void getAllUsingCofigreq(DaprPreviewClient client) {
    System.out.println("trying to fetch using getAllUsingCofigreq...");
    GetBulkConfigurationRequest req = new GetBulkConfigurationRequest(configurationStoreName, null);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(req);
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void getAllUsingVarargs(DaprPreviewClient client) {
    System.out.println("trying to fetch using getAllUsingVarargs...");
    try {
      Mono<List<ConfigurationItem>> items = client.getConfigurations(configurationStoreName);
      items.block().forEach(ConfigurationClientSDK::print);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  public static void errorUsingParameters(DaprPreviewClient client) {
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
