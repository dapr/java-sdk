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

package io.dapr.testcontainers.wait.strategy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.testcontainers.wait.strategy.metadata.Metadata;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Base wait strategy for Dapr containers that polls the metadata endpoint.
 * Subclasses implement specific conditions to wait for.
 */
public abstract class DaprWaitStrategy extends AbstractWaitStrategy {

  private static final int DAPR_HTTP_PORT = 3500;
  private static final String METADATA_ENDPOINT = "/v1.0/metadata";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private Duration pollInterval = Duration.ofMillis(500);

  /**
   * Sets the poll interval for checking the metadata endpoint.
   *
   * @param pollInterval the interval between polling attempts
   * @return this strategy for chaining
   */
  public DaprWaitStrategy withPollInterval(Duration pollInterval) {
    this.pollInterval = pollInterval;
    return this;
  }

  @Override
  protected void waitUntilReady() {
    String host = waitStrategyTarget.getHost();
    Integer port = waitStrategyTarget.getMappedPort(DAPR_HTTP_PORT);
    String metadataUrl = String.format("http://%s:%d%s", host, port, METADATA_ENDPOINT);

    try {
      Awaitility.await()
          .atMost(startupTimeout.getSeconds(), TimeUnit.SECONDS)
          .pollInterval(pollInterval.toMillis(), TimeUnit.MILLISECONDS)
          .ignoreExceptions()
          .until(() -> checkCondition(metadataUrl));
    } catch (Exception e) {
      throw new ContainerLaunchException(
          String.format("Timed out waiting for Dapr condition: %s", getConditionDescription()), e);
    }
  }

  /**
   * Checks if the wait condition is satisfied.
   *
   * @param metadataUrl the URL to the metadata endpoint
   * @return true if the condition is met
   * @throws IOException if there's an error fetching metadata
   */
  protected boolean checkCondition(String metadataUrl) throws IOException {
    Metadata metadata = fetchMetadata(metadataUrl);
    return isConditionMet(metadata);
  }

  /**
   * Fetches metadata from the Dapr sidecar.
   *
   * @param metadataUrl the URL to fetch metadata from
   * @return the parsed metadata
   * @throws IOException if there's an error fetching or parsing
   */
  protected Metadata fetchMetadata(String metadataUrl) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(metadataUrl).openConnection();
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(1000);
    connection.setReadTimeout(1000);

    try {
      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        throw new IOException("Metadata endpoint returned status: " + responseCode);
      }
      return OBJECT_MAPPER.readValue(connection.getInputStream(), Metadata.class);
    } finally {
      connection.disconnect();
    }
  }

  /**
   * Checks if the specific wait condition is met based on the metadata.
   *
   * @param metadata the current Dapr metadata
   * @return true if the condition is satisfied
   */
  protected abstract boolean isConditionMet(Metadata metadata);

  /**
   * Returns a description of what this strategy is waiting for.
   *
   * @return a human-readable description of the condition
   */
  protected abstract String getConditionDescription();

  /**
   * Creates a predicate-based wait strategy for custom conditions.
   *
   * @param predicate the predicate to test against metadata
   * @param description a description of what the predicate checks
   * @return a new wait strategy
   */
  public static DaprWaitStrategy forCondition(Predicate<Metadata> predicate, String description) {
    return new DaprWaitStrategy() {
      @Override
      protected boolean isConditionMet(Metadata metadata) {
        return predicate.test(metadata);
      }

      @Override
      protected String getConditionDescription() {
        return description;
      }
    };
  }
}
