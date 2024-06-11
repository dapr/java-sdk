/*
 * Copyright 2022 The Dapr Authors
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

import java.util.Objects;

/**
 * ComponentMetadata describes a Dapr Component.
 */
public final class ComponentMetadata {

  private String name;
  private String type;
  private String version;

  public ComponentMetadata() {
  }

  /**
   * Constructor for a ComponentMetadata.
   *
   * @param name of the component
   * @param type component type
   * @param version version of the component
   */
  public ComponentMetadata(String name, String type, String version) {
    this.name = name;
    this.type = type;
    this.version = version;
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

  @Override
  public int hashCode() {
    return Objects.hash(name, type, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ComponentMetadata other = (ComponentMetadata) obj;
    return Objects.equals(name, other.name) && Objects.equals(type, other.type)
        && Objects.equals(version, other.version);
  }
  
}
