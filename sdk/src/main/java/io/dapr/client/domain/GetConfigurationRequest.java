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
import java.util.List;
import java.util.Map;

/**
 * Request to get one or more configuration items from Dapr's configuration store.
 */
public class GetConfigurationRequest {
  private final String storeName;
  private final List<String> keys;
  private Map<String, String> metadata;

  /**
   * Constructor for GetConfigurationRequest.
   *
   * @param storeName Name of the configuration store
   * @param keys      Keys for the configuration objects
   */
  public GetConfigurationRequest(String storeName, List<String> keys) {
    this.storeName = storeName;
    this.keys = keys == null ? Collections.emptyList() : Collections.unmodifiableList(keys);
  }

  public GetConfigurationRequest setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public String getStoreName() {
    return storeName;
  }

  public List<String> getKeys() {
    return keys;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
