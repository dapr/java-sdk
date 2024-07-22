/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.testcontainers;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

class YamlRepresenter extends Representer {
  public YamlRepresenter(DumperOptions options) {
    super(options);
    this.representers.put(QuotedBoolean.class, new RepresentQuotedBoolean());
  }

  private class RepresentQuotedBoolean implements Represent {
    public Node representData(Object data) {
      QuotedBoolean str = (QuotedBoolean) data;
      return representScalar(Tag.STR, str.getValue(), DumperOptions.ScalarStyle.DOUBLE_QUOTED);
    }
  }
}
