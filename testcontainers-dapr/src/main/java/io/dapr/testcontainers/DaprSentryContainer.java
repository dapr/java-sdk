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

import io.dapr.testcontainers.converter.ConfigurationYamlConverter;
import io.dapr.testcontainers.converter.YamlConverter;
import io.dapr.testcontainers.converter.YamlMapperFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Dapr Sentry container. Sentry is the certificate authority of the Dapr control plane and is required to
 * enable mTLS. The mTLS settings are read from the {@code spec.mtls} section of the {@link Configuration}
 * passed to this container.
 */
public class DaprSentryContainer extends GenericContainer<DaprSentryContainer> {

  public static final int DEFAULT_SENTRY_PORT = 50001;
  public static final int DEFAULT_HEALTHZ_PORT = 8080;
  public static final String DEFAULT_TRUST_DOMAIN = "localhost";
  public static final String DEFAULT_ISSUER_CREDENTIALS_PATH = "/var/run/secrets/dapr.io/credentials";

  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("daprio/sentry");
  private static final String CONFIGURATION_PATH = "/dapr-resources/";
  private static final String TRUST_ANCHORS_FILENAME = "ca.crt";
  private static final YamlConverter<Configuration> CONFIGURATION_CONVERTER =
      new ConfigurationYamlConverter(YamlMapperFactory.create());

  private int sentryPort = DEFAULT_SENTRY_PORT;
  private int healthzPort = DEFAULT_HEALTHZ_PORT;
  private String trustDomain = DEFAULT_TRUST_DOMAIN;
  private String issuerCredentialsPath = DEFAULT_ISSUER_CREDENTIALS_PATH;
  private DaprLogLevel daprLogLevel = DaprLogLevel.INFO;
  private Configuration configuration;

  /**
   * Creates a new Dapr sentry container.
   * @param dockerImageName Docker image name.
   */
  public DaprSentryContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
  }

  /**
   * Creates a new Dapr sentry container.
   * @param image Docker image name.
   */
  public DaprSentryContainer(String image) {
    this(DockerImageName.parse(image));
  }

  @Override
  protected void configure() {
    super.configure();

    withExposedPorts(sentryPort, healthzPort);

    // Sentry generates a self-signed CA on first start when no issuer credentials are found. It runs as a
    // non-root user, so the credentials directory must exist and be writable. After generating the
    // credentials it reloads itself, so we wait until it reports the credentials were loaded from the store.
    withCopyToContainer(Transferable.of("", 0777), issuerCredentialsPath + "/");
    setWaitStrategy(new WaitAllStrategy()
        .withStrategy(Wait.forLogMessage(".*Root and issuer certs found: using credentials from store.*", 1))
        .withStrategy(Wait.forHttp("/healthz").forPort(healthzPort)));

    List<String> cmds = new ArrayList<>();
    cmds.add("./sentry");
    cmds.add("--port");
    cmds.add(Integer.toString(sentryPort));
    cmds.add("--healthz-port");
    cmds.add(Integer.toString(healthzPort));
    cmds.add("--trust-domain");
    cmds.add(trustDomain);
    cmds.add("--issuer-credentials");
    cmds.add(issuerCredentialsPath);
    cmds.add("--log-level");
    cmds.add(daprLogLevel.toString());

    if (configuration != null) {
      String configurationYaml = CONFIGURATION_CONVERTER.convert(configuration);
      String configurationFile = CONFIGURATION_PATH + configuration.getName() + ".yaml";

      withCopyToContainer(Transferable.of(configurationYaml), configurationFile);
      cmds.add("--config");
      cmds.add(configurationFile);
    }

    withCommand(cmds.toArray(new String[]{}));
  }

  public static DockerImageName getDefaultImageName() {
    return DEFAULT_IMAGE_NAME;
  }

  /**
   * Sets the Dapr {@link Configuration} used by sentry. Its {@code spec.mtls} section configures the
   * certificate authority (workload certificate TTL, allowed clock skew, token validators, etc.).
   * @param configuration Dapr configuration.
   * @return this container.
   */
  public DaprSentryContainer withConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }

  public DaprSentryContainer withPort(Integer port) {
    this.sentryPort = port;
    return this;
  }

  public DaprSentryContainer withHealthzPort(Integer healthzPort) {
    this.healthzPort = healthzPort;
    return this;
  }

  public DaprSentryContainer withTrustDomain(String trustDomain) {
    this.trustDomain = trustDomain;
    return this;
  }

  public DaprSentryContainer withIssuerCredentialsPath(String issuerCredentialsPath) {
    this.issuerCredentialsPath = issuerCredentialsPath;
    return this;
  }

  public DaprSentryContainer withDaprLogLevel(DaprLogLevel daprLogLevel) {
    this.daprLogLevel = daprLogLevel;
    return this;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public int getPort() {
    return sentryPort;
  }

  public int getHealthzPort() {
    return healthzPort;
  }

  public String getTrustDomain() {
    return trustDomain;
  }

  public String getIssuerCredentialsPath() {
    return issuerCredentialsPath;
  }

  /**
   * Returns the PEM encoded root CA certificate (trust anchors) issued by this sentry instance.
   * The container must be running.
   * @return PEM encoded trust anchors.
   */
  public String getTrustAnchors() {
    if (!isRunning()) {
      throw new IllegalStateException("Sentry container must be running to read the trust anchors");
    }

    return copyFileFromContainer(issuerCredentialsPath + "/" + TRUST_ANCHORS_FILENAME,
        inputStream -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
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
