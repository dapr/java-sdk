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
 * Access rules limiting which secrets of a given secret store the application can read.
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/secret-scope/">
 *   Dapr secret store scoping</a>
 */
public class SecretScope {
  private final String storeName;
  private final SecretScopeAccess defaultAccess;
  private final List<String> allowedSecrets;
  private final List<String> deniedSecrets;

  /**
   * Creates a new secret scope.
   *
   * @param storeName     name of the secret store component the scope applies to.
   * @param defaultAccess default access to the secrets of the store; Dapr assumes {@code allow} when not set.
   */
  public SecretScope(String storeName, SecretScopeAccess defaultAccess) {
    this(storeName, defaultAccess, null, null);
  }

  /**
   * Creates a new secret scope.
   *
   * @param storeName      name of the secret store component the scope applies to.
   * @param defaultAccess  default access to the secrets of the store; Dapr assumes {@code allow} when not set.
   * @param allowedSecrets secrets the application is allowed to read, taking precedence over {@code defaultAccess}.
   * @param deniedSecrets  secrets the application is not allowed to read, taking precedence over
   *                       {@code defaultAccess}.
   */
  public SecretScope(
      String storeName,
      SecretScopeAccess defaultAccess,
      List<String> allowedSecrets,
      List<String> deniedSecrets
  ) {
    this.storeName = storeName;
    this.defaultAccess = defaultAccess;
    this.allowedSecrets = allowedSecrets != null ? Collections.unmodifiableList(new ArrayList<>(allowedSecrets)) : null;
    this.deniedSecrets = deniedSecrets != null ? Collections.unmodifiableList(new ArrayList<>(deniedSecrets)) : null;
  }

  public String getStoreName() {
    return storeName;
  }

  public SecretScopeAccess getDefaultAccess() {
    return defaultAccess;
  }

  public List<String> getAllowedSecrets() {
    return allowedSecrets;
  }

  public List<String> getDeniedSecrets() {
    return deniedSecrets;
  }
}
