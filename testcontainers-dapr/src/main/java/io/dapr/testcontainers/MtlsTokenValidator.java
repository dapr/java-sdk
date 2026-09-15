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

import java.util.Collections;
import java.util.Map;

/**
 * Additional Sentry token validator used to authenticate certificate requests (mTLS {@code tokenValidators} entry).
 *
 * <p>Only the {@code jwks} validator can be configured manually. The built-in {@code kubernetes} and
 * {@code insecure} validators are enabled automatically by Sentry depending on the runtime mode and are
 * rejected when listed in {@code tokenValidators}. In self-hosted mode, configuring a validator disables the
 * built-in {@code insecure} validator.
 */
public class MtlsTokenValidator {
  /**
   * Name of the JWKS validator, the only validator that can be configured manually.
   */
  public static final String JWKS = "jwks";

  private final String name;
  private final Map<String, String> options;

  /**
   * Creates a token validator.
   *
   * @param name    validator name. Only {@link #JWKS} is accepted by Sentry.
   * @param options validator options, if any (e.g. {@code source}, {@code minRefreshInterval},
   *                {@code requestTimeout}, {@code caCertificate} for the JWKS validator).
   */
  public MtlsTokenValidator(String name, Map<String, String> options) {
    this.name = name;
    this.options = options != null ? Collections.unmodifiableMap(options) : null;
  }

  /**
   * Creates a JWKS token validator.
   *
   * @param options JWKS validator options (e.g. {@code source}, {@code minRefreshInterval},
   *                {@code requestTimeout}, {@code caCertificate}).
   * @return a JWKS token validator.
   */
  public static MtlsTokenValidator jwks(Map<String, String> options) {
    return new MtlsTokenValidator(JWKS, options);
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getOptions() {
    return options;
  }
}
