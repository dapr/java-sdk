/*
 * Copyright 2024 The Dapr Authors
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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

/**
 * Test container for Dapr placement service.
 */
public class DaprPlacementContainer extends GenericContainer<DaprPlacementContainer> {

  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("daprio/placement");
  private static final String TRUST_ANCHORS_FILE = "/var/run/secrets/dapr.io/tls/ca.crt";
  private int placementPort = 50005;
  private boolean tlsEnabled;
  private String sentryAddress;
  private String trustDomain;
  private String trustAnchors;

  /**
   * Creates a new Dapr placement container.
   * @param dockerImageName Docker image name.
   */
  public DaprPlacementContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

    withExposedPorts(placementPort);
  }

  /**
   * Creates a new Dapr placement container.
   * @param image Docker image name.
   */
  public DaprPlacementContainer(String image) {
    this(DockerImageName.parse(image));
  }

  @Override
  protected void configure() {
    super.configure();
    List<String> cmds = new ArrayList<>();
    cmds.add("./placement");
    cmds.add("-port");
    cmds.add(Integer.toString(placementPort));
    cmds.addAll(tlsCommandArguments());

    withCommand(cmds.toArray(new String[]{}));
  }

  private List<String> tlsCommandArguments() {
    List<String> cmds = new ArrayList<>();

    if (!tlsEnabled) {
      return cmds;
    }

    cmds.add("--tls-enabled");

    if (sentryAddress != null) {
      cmds.add("--sentry-address");
      cmds.add(sentryAddress);
    }

    if (trustDomain != null) {
      cmds.add("--trust-domain");
      cmds.add(trustDomain);
    }

    if (trustAnchors != null) {
      withCopyToContainer(Transferable.of(trustAnchors), TRUST_ANCHORS_FILE);
      cmds.add("--trust-anchors-file");
      cmds.add(TRUST_ANCHORS_FILE);
    }

    return cmds;
  }

  public static DockerImageName getDefaultImageName() {
    return DEFAULT_IMAGE_NAME;
  }

  public DaprPlacementContainer withPort(Integer port) {
    this.placementPort = port;
    return this;
  }

  public int getPort() {
    return placementPort;
  }

  /**
   * Enables TLS on the placement gRPC server. Requires a Sentry address and the trust anchors issued by Sentry.
   * @param tlsEnabled whether TLS is enabled.
   * @return this container.
   */
  public DaprPlacementContainer withTlsEnabled(boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
    return this;
  }

  public DaprPlacementContainer withSentryAddress(String sentryAddress) {
    this.sentryAddress = sentryAddress;
    return this;
  }

  public DaprPlacementContainer withTrustDomain(String trustDomain) {
    this.trustDomain = trustDomain;
    return this;
  }

  /**
   * Sets the PEM encoded trust anchors (root CA certificate) issued by Sentry.
   * @param trustAnchors PEM encoded trust anchors.
   * @return this container.
   */
  public DaprPlacementContainer withTrustAnchors(String trustAnchors) {
    this.trustAnchors = trustAnchors;
    return this;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public String getSentryAddress() {
    return sentryAddress;
  }

  public String getTrustDomain() {
    return trustDomain;
  }

  // Required by spotbugs plugin
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
