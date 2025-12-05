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

package io.dapr.spring.boot.cloudconfig.configdata.secret;

import io.dapr.spring.boot.cloudconfig.configdata.types.DaprCloudConfigType;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.lang.Nullable;

public class DaprSecretStoreConfigDataResource extends ConfigDataResource {
  private final String storeName;
  private final String secretName;
  private final DaprCloudConfigType type;

  /**
   * Create a new non-optional {@link ConfigDataResource} instance.
   *
   * @param storeName store name
   * @param secretName secret name
   * @param type secret type
   */
  public DaprSecretStoreConfigDataResource(String storeName, @Nullable String secretName, DaprCloudConfigType type) {
    this.storeName = storeName;
    this.secretName = secretName;
    this.type = type;
  }

  /**
   * Create a new {@link ConfigDataResource} instance.
   *
   * @param optional if the resource is optional
   * @param storeName store name
   * @param secretName secret name
   * @param type secret type
   */
  public DaprSecretStoreConfigDataResource(boolean optional, String storeName,
                                           @Nullable String secretName, DaprCloudConfigType type) {
    super(optional);
    this.storeName = storeName;
    this.secretName = secretName;
    this.type = type;
  }

  public String getStoreName() {
    return storeName;
  }

  public String getSecretName() {
    return secretName;
  }

  public DaprCloudConfigType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "DaprSecretStoreConfigDataResource{"
        + "storeName='" + storeName + '\''
        + ", secretName='" + secretName + '\''
        + ", type=" + type
        + '}';
  }
}
