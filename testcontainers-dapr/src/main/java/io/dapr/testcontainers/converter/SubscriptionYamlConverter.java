/*
 * Copyright 2021 The Dapr Authors
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
