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

package io.dapr.client.domain;

import java.util.Collections;
import java.util.List;

/**
 * ComponentMetadata describes a Dapr Component.
 */
public final class ComponentMetadata {

  private final String name;
  private final String type;
  private final String version;
  private final List<String> capabilities;

  /**
   * Constructor for a ComponentMetadata.
   *
   * @param name of the component
   * @param type component type
   * @param version version of the component
   * @param capabilities capabilities of the component
   */
  public ComponentMetadata(String name, String type, String version, List<String> capabilities) {
    this.name = name;
    this.type = type;
    this.version = version;
    this.capabilities = capabilities == null ? Collections.emptyList() : Collections.unmodifiableList(capabilities);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getVersion() {
    return version;
  }

  public List<String> getCapabilities() {
    return capabilities;
  }

}
