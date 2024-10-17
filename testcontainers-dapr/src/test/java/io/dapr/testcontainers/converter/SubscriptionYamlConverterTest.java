package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.Subscription;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionYamlConverterTest {
  private final Yaml MAPPER = YamlMapperFactory.create();
  private final SubscriptionYamlConverter converter = new SubscriptionYamlConverter(MAPPER);

  @Test
  public void testSubscriptionToYaml() {
    DaprContainer dapr = new DaprContainer("daprio/daprd")
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withSubscription(new Subscription("my-subscription", "pubsub", "topic", "/events"))
        .withAppChannelAddress("host.testcontainers.internal");

    Set<Subscription> subscriptions = dapr.getSubscriptions();
    assertEquals(1, subscriptions.size());

    Subscription subscription = subscriptions.iterator().next();
    String subscriptionYaml = converter.convert(subscription);
    String expectedSubscriptionYaml =
          "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Subscription\n"
        + "metadata:\n"
        + "  name: my-subscription\n"
        + "spec:\n"
        + "  pubsubname: pubsub\n"
        + "  topic: topic\n"
        + "  route: /events\n";

    assertEquals(expectedSubscriptionYaml, subscriptionYaml);
  }
}
