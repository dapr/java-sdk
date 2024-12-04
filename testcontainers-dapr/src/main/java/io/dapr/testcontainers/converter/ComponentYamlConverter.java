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
