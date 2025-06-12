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

import io.dapr.testcontainers.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComponentYamlConverter implements YamlConverter<Component> {
  private final Yaml mapper;

  public ComponentYamlConverter(Yaml mapper) {
    this.mapper = mapper;
  }

  @Override
  public String convert(Component component) {
    Map<String, Object> componentProps = new LinkedHashMap<>();
    componentProps.put("apiVersion", "dapr.io/v1alpha1");
    componentProps.put("kind", "Component");

    Map<String, String> componentMetadata = new LinkedHashMap<>();
    componentMetadata.put("name", component.getName());
    componentProps.put("metadata", componentMetadata);

    Map<String, Object> componentSpec = new LinkedHashMap<>();
    componentSpec.put("type", component.getType());
    componentSpec.put("version", component.getVersion());

    if (!component.getMetadata().isEmpty()) {
      componentSpec.put("metadata", component.getMetadata());
    }

    componentProps.put("spec", componentSpec);

    return mapper.dumpAsMap(componentProps);
  }
}
