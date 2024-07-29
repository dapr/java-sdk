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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Dapr component.
 */
public class Component {
  private String name;
  private String type;
  private String version;
  private List<MetadataEntry> metadata;

  /**
   * Creates a new component.
   *
   * @param name     Component name.
   * @param type     Component type.
   * @param version  Component version.
   * @param metadata Metadata.
   */
  public Component(String name, String type, String version, Map<String, String> metadata) {
    this.name = name;
    this.type = type;
    this.version = version;

    List<MetadataEntry> entries = new ArrayList<>();
    if (!metadata.isEmpty()) {
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        entries.add(new MetadataEntry(entry.getKey(), entry.getValue()));
      }
    }
    this.metadata = Collections.unmodifiableList(entries);
  }

  /**
   * Creates a new component.
   *
   * @param name            Component name.
   * @param type            Component type.
   * @param version         Component version.
   * @param metadataEntries Component metadata entries.
   */
  public Component(String name, String type, String version, List<MetadataEntry> metadataEntries) {
    this.name = name;
    this.type = type;
    this.version = version;
    metadata = Objects.requireNonNull(metadataEntries);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public List<MetadataEntry> getMetadata() {
    return metadata;
  }

  public String getVersion() {
    return version;
  }
}
