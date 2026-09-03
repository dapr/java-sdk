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

package io.dapr.testcontainers;

import java.util.Collections;
import java.util.Map;

/**
 * Additional Sentry token validator used to authenticate certificate requests (mTLS tokenValidators entry).
 */
public class MtlsTokenValidator {
  private final String name;
  private final Map<String, String> options;

  /**
   * Creates a token validator without options.
   *
   * @param name validator name (e.g. "jwks", "kubernetes", "insecure").
   */
  public MtlsTokenValidator(String name) {
    this(name, null);
  }

  /**
   * Creates a token validator.
   *
   * @param name    validator name (e.g. "jwks", "kubernetes", "insecure").
   * @param options validator options, if any.
   */
  public MtlsTokenValidator(String name, Map<String, String> options) {
    this.name = name;
    this.options = options != null ? Collections.unmodifiableMap(options) : null;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getOptions() {
    return options;
  }
}
