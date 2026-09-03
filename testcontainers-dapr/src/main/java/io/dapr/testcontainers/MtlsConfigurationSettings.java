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
import java.util.List;

/**
 * Configuration settings for mTLS (mutual TLS).
 *
 * @see <a href="https://docs.dapr.io/operations/configuration/configuration-overview/#mtls-mutual-tls">
 *   Dapr mTLS configuration</a>
 */
public class MtlsConfigurationSettings implements ConfigurationSettings {
  private final Boolean enabled;
  private final String workloadCertTtl;
  private final String allowedClockSkew;
  private final String sentryAddress;
  private final String controlPlaneTrustDomain;
  private final List<MtlsTokenValidator> tokenValidators;

  /**
   * Creates a new mTLS configuration.
   *
   * @param enabled          if true, enables mTLS for communication between services and apps.
   * @param workloadCertTtl  how long a TLS certificate issued by Dapr is valid for (Go duration, e.g. "24h").
   * @param allowedClockSkew allowed tolerance when checking certificate expiration (Go duration, e.g. "15m").
   */
  public MtlsConfigurationSettings(Boolean enabled, String workloadCertTtl, String allowedClockSkew) {
    this(enabled, workloadCertTtl, allowedClockSkew, null, null, null);
  }

  /**
   * Creates a new mTLS configuration.
   *
   * @param enabled                 if true, enables mTLS for communication between services and apps.
   * @param workloadCertTtl         how long a TLS certificate issued by Dapr is valid for
   *                                (Go duration, e.g. "24h").
   * @param allowedClockSkew        allowed tolerance when checking certificate expiration
   *                                (Go duration, e.g. "15m").
   * @param sentryAddress           hostname port address for connecting to the Sentry server.
   * @param controlPlaneTrustDomain trust domain for the control plane.
   */
  public MtlsConfigurationSettings(
      Boolean enabled,
      String workloadCertTtl,
      String allowedClockSkew,
      String sentryAddress,
      String controlPlaneTrustDomain
  ) {
    this(enabled, workloadCertTtl, allowedClockSkew, sentryAddress, controlPlaneTrustDomain, null);
  }

  /**
   * Creates a new mTLS configuration.
   *
   * @param enabled                 if true, enables mTLS for communication between services and apps.
   * @param workloadCertTtl         how long a TLS certificate issued by Dapr is valid for
   *                                (Go duration, e.g. "24h").
   * @param allowedClockSkew        allowed tolerance when checking certificate expiration
   *                                (Go duration, e.g. "15m").
   * @param sentryAddress           hostname port address for connecting to the Sentry server.
   * @param controlPlaneTrustDomain trust domain for the control plane.
   * @param tokenValidators         additional Sentry token validators used to authenticate
   *                                certificate requests.
   */
  public MtlsConfigurationSettings(
      Boolean enabled,
      String workloadCertTtl,
      String allowedClockSkew,
      String sentryAddress,
      String controlPlaneTrustDomain,
      List<MtlsTokenValidator> tokenValidators
  ) {
    this.enabled = enabled;
    this.workloadCertTtl = workloadCertTtl;
    this.allowedClockSkew = allowedClockSkew;
    this.sentryAddress = sentryAddress;
    this.controlPlaneTrustDomain = controlPlaneTrustDomain;
    this.tokenValidators = tokenValidators != null ? Collections.unmodifiableList(tokenValidators) : null;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public String getWorkloadCertTtl() {
    return workloadCertTtl;
  }

  public String getAllowedClockSkew() {
    return allowedClockSkew;
  }

  public String getSentryAddress() {
    return sentryAddress;
  }

  public String getControlPlaneTrustDomain() {
    return controlPlaneTrustDomain;
  }

  public List<MtlsTokenValidator> getTokenValidators() {
    return tokenValidators;
  }
}
