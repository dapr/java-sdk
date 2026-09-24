/*
 * Copyright 2026 The Dapr Authors
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

/**
 * Configuration settings limiting the secrets the application can access from secret stores.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#scope-secret-store-access">
 *   Dapr scope secret store access</a>
 */
public class SecretsConfigurationSettings implements ConfigurationSettings {
  private final List<SecretScope> scopes;

  /**
   * Creates a new secrets configuration.
   *
   * @param scopes access rules applied to each secret store.
   */
  public SecretsConfigurationSettings(List<SecretScope> scopes) {
    this.scopes = scopes != null ? Collections.unmodifiableList(new ArrayList<>(scopes)) : null;
  }

  public List<SecretScope> getScopes() {
    return scopes;
  }
}
