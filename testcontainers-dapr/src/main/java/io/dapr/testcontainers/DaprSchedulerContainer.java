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

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test container for Dapr scheduler service.
 */
public class DaprSchedulerContainer extends GenericContainer<DaprSchedulerContainer> {

  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("daprio/scheduler");
  private static final String TRUST_ANCHORS_FILE = "/var/run/secrets/dapr.io/tls/ca.crt";
  private int schedulerPort = 51005;
  private boolean tlsEnabled;
  private String sentryAddress;
  private String trustDomain;
  private String trustAnchors;

  /**
   * Creates a new Dapr scheduler container.
   * @param dockerImageName Docker image name.
   */
  public DaprSchedulerContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    withExposedPorts(schedulerPort);
  }

  /**
   * Creates a new Dapr schedulers container.
   * @param image Docker image name.
   */
  public DaprSchedulerContainer(String image) {
    this(DockerImageName.parse(image));
  }

  @Override
  protected void configure() {
    super.configure();

    withCopyToContainer(Transferable.of("", 0777), "./default-dapr-scheduler-server-0/dapr-0.1/");
    withCopyToContainer(Transferable.of("", 0777), "./dapr-scheduler-existing-cluster/");
    List<String> cmds = new ArrayList<>();
    cmds.add("./scheduler");
    cmds.add("--port");
    cmds.add(Integer.toString(schedulerPort));
    cmds.add("--etcd-data-dir");
    cmds.add(".");
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

  public DaprSchedulerContainer withPort(Integer port) {
    this.schedulerPort = port;
    return this;
  }

  public int getPort() {
    return schedulerPort;
  }

  /**
   * Enables TLS on the scheduler gRPC server. Requires a Sentry address and the trust anchors issued by Sentry.
   * @param tlsEnabled whether TLS is enabled.
   * @return this container.
   */
  public DaprSchedulerContainer withTlsEnabled(boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
    return this;
  }

  public DaprSchedulerContainer withSentryAddress(String sentryAddress) {
    this.sentryAddress = sentryAddress;
    return this;
  }

  public DaprSchedulerContainer withTrustDomain(String trustDomain) {
    this.trustDomain = trustDomain;
    return this;
  }

  /**
   * Sets the PEM encoded trust anchors (root CA certificate) issued by Sentry.
   * @param trustAnchors PEM encoded trust anchors.
   * @return this container.
   */
  public DaprSchedulerContainer withTrustAnchors(String trustAnchors) {
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
