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

import io.dapr.testcontainers.ListEntry;
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
    representer.addClassTag(ListEntry.class, Tag.MAP);
    return new Yaml(representer);
  }
}
