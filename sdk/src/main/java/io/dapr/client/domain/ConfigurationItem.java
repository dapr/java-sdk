/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Map;

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
    this.metadata = metadata;
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
