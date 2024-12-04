package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.Subscription;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

public class SubscriptionYamlConverter implements YamlConverter<Subscription> {
  private final Yaml mapper;

  public SubscriptionYamlConverter(Yaml mapper) {
    this.mapper = mapper;
  }

  @Override
  public String convert(Subscription subscription) {
    Map<String, Object> subscriptionProps = new LinkedHashMap<>();
    subscriptionProps.put("apiVersion", "dapr.io/v1alpha1");
    subscriptionProps.put("kind", "Subscription");

    Map<String, String> subscriptionMetadata = new LinkedHashMap<>();
    subscriptionMetadata.put("name", subscription.getName());
    subscriptionProps.put("metadata", subscriptionMetadata);

    Map<String, Object> subscriptionSpec = new LinkedHashMap<>();
    subscriptionSpec.put("pubsubname", subscription.getPubsubName());
    subscriptionSpec.put("topic", subscription.getTopic());
    subscriptionSpec.put("route", subscription.getRoute());

    subscriptionProps.put("spec", subscriptionSpec);

    return mapper.dumpAsMap(subscriptionProps);
  }
}
