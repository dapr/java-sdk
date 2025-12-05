/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.boot.cloudconfig.configdata.config;

import io.dapr.spring.boot.cloudconfig.configdata.types.DaprCloudConfigType;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.lang.Nullable;

public class DaprConfigurationConfigDataResource extends ConfigDataResource {
  private final String storeName;
  private final String configName;
  private final DaprCloudConfigType type;
  private final Boolean subscribe;

  /**
   * Create a new non-optional {@link ConfigDataResource} instance.
   * @param storeName store name
   * @param configName config name
   * @param type value type
   * @param subscribe subscribe for update
   */
  public DaprConfigurationConfigDataResource(String storeName, @Nullable String configName,
                                             DaprCloudConfigType type, Boolean subscribe) {
    this.storeName = storeName;
    this.configName = configName;
    this.type = type;
    this.subscribe = subscribe;
  }

  /**
   * Create a new {@link ConfigDataResource} instance.
   * @param optional if the resource is optional
   * @param storeName store name
   * @param configName config name
   * @param type value type
   * @param subscribe subscribe for update
   */
  public DaprConfigurationConfigDataResource(boolean optional, String storeName, @Nullable String configName,
                                             DaprCloudConfigType type, Boolean subscribe) {
    super(optional);
    this.storeName = storeName;
    this.configName = configName;
    this.type = type;
    this.subscribe = subscribe;
  }

  public String getStoreName() {
    return storeName;
  }

  public String getConfigName() {
    return configName;
  }

  public DaprCloudConfigType getType() {
    return type;
  }
}
