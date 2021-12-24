package io.dapr.examples.configuration.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.SubscribeConfigurationRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SubscribeConfigurationClient {
  private static final String configurationStoreName = "redisconfigstore";
  private static List<String> k = new ArrayList<>(Arrays.asList("myconfig2","myconfig3", "myconfig4"));

  public static void main(String[] args) throws Exception {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      subscribeConfigurationRequestWithIterator(client);
      subscribeConfigurationRequestDemoWithSubscribe(client);
    }
  }

  public static void subscribeConfigurationRequestWithIterator(DaprClient client) {
    System.out.println("Subscribing to keys: " + k.toString());
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(configurationStoreName, k);
    Iterator<List<ConfigurationItem>> itr = client.subscribeToConfigurations(req).toIterable().iterator();
    while(itr.hasNext()) {
      List<ConfigurationItem> cis = itr.next();
      cis.forEach(SubscribeConfigurationClient::print);
    }
  }

  public static void subscribeConfigurationRequestDemoWithSubscribe(DaprClient client) {
    System.out.println("Subscribing to keys using subscribe method: " + k.toString());
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(configurationStoreName, k);
    client.subscribeToConfigurations(req)
        .subscribe(
            cis -> cis.forEach(SubscribeConfigurationClient::print)
        );
    while (true) {}
  }

  private static void print(ConfigurationItem item) {
    System.out.println(item.getValue() + " : key ->" + item.getKey());
  }
}
