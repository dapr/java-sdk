package io.dapr.testcontainers.converter;

import io.dapr.testcontainers.MetadataEntry;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Factory for creating a YAML mapper.
 */
public class YamlMapperFactory {

  /**
   * Creates a YAML mapper.
   * @return YAML mapper.
   */
  public static Yaml create() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Representer representer = new Representer(options);
    representer.addClassTag(MetadataEntry.class, Tag.MAP);
    return new Yaml(representer);
  }
}
