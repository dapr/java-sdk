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

import java.util.Collections;
import java.util.Map;

/**
 * A configuration item from Dapr's configuration store.
 */
public class ConfigurationItem {
  private final String key;
  private final String value;
  private final String version;
  private final Map<String, String> metadata;

  /**
   * Constructor for a configuration Item.
   *
   * @param key key of the configuration item
   * @param value value for the provided key
   * @param version version of the configuration item
   * @param metadata additional information
   */
  public ConfigurationItem(String key, String value, String version, Map<String, String> metadata) {
    this.key = key;
    this.value = value;
    this.version = version;
    this.metadata = Collections.unmodifiableMap(metadata);
  }

  /**
   * Constructor for a configuration Item.
   *
   * @param key key of the configuration item
   * @param value value for the provided key
   * @param version version of the configuration item
   */
  public ConfigurationItem(String key, String value, String version) {
    this.key = key;
    this.value = value;
    this.version = version;
    this.metadata = Collections.emptyMap();
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
