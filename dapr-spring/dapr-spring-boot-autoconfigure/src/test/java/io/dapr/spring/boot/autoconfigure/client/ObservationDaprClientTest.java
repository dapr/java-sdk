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

package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.client.domain.State;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ObservationDaprClient}.
 *
 * <p>Verifies two key requirements:
 * <ol>
 *   <li>Each non-deprecated method creates a correctly named Micrometer Observation span.</li>
 *   <li>The wrapper is fully transparent: it implements {@link DaprClient}, so consumers
 *       keep injecting {@code DaprClient} without any code changes.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ObservationDaprClientTest {

  @Mock
  private DaprClient delegate;

  private TestObservationRegistry registry;
  private ObservationDaprClient client;

  @BeforeEach
  void setUp() {
    registry = TestObservationRegistry.create();
    client = new ObservationDaprClient(delegate, registry);
  }

  // -------------------------------------------------------------------------
  // Transparency — the wrapper IS-A DaprClient
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("ObservationDaprClient is assignable to DaprClient (transparent to consumers)")
  void isAssignableToDaprClient() {
    assertThat(client).isInstanceOf(DaprClient.class);
  }

  // -------------------------------------------------------------------------
  // Pub/Sub
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("publishEvent(pubsubName, topicName, data) creates span dapr.client.publish_event")
  void publishEventCreatesSpan() {
    when(delegate.publishEvent("my-pubsub", "my-topic", "payload")).thenReturn(Mono.empty());

    client.publishEvent("my-pubsub", "my-topic", "payload").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.publish_event")
        .that()
        .hasHighCardinalityKeyValue("dapr.pubsub.name", "my-pubsub")
        .hasHighCardinalityKeyValue("dapr.topic.name", "my-topic");
  }

  @Test
  @DisplayName("publishEvent(PublishEventRequest) creates span dapr.client.publish_event")
  void publishEventRequestCreatesSpan() {
    PublishEventRequest request = new PublishEventRequest("my-pubsub", "my-topic", "payload");
    when(delegate.publishEvent(request)).thenReturn(Mono.empty());

    client.publishEvent(request).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.publish_event");
  }

  @Test
  @DisplayName("publishEvent span records error when delegate throws")
  void publishEventRecordsError() {
    RuntimeException boom = new RuntimeException("publish failed");
    when(delegate.publishEvent("pubsub", "topic", "data")).thenReturn(Mono.error(boom));

    assertThatThrownBy(() -> client.publishEvent("pubsub", "topic", "data").block())
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.publish_event")
        .that()
        .hasError();
  }

  // -------------------------------------------------------------------------
  // Bindings
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("invokeBinding(name, operation, data) creates span dapr.client.invoke_binding")
  void invokeBindingCreatesSpan() {
    when(delegate.invokeBinding("my-binding", "create", "data")).thenReturn(Mono.empty());

    client.invokeBinding("my-binding", "create", "data").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.invoke_binding")
        .that()
        .hasHighCardinalityKeyValue("dapr.binding.name", "my-binding")
        .hasHighCardinalityKeyValue("dapr.binding.operation", "create");
  }

  @Test
  @DisplayName("invokeBinding(InvokeBindingRequest) creates span dapr.client.invoke_binding")
  void invokeBindingRequestCreatesSpan() {
    InvokeBindingRequest request = new InvokeBindingRequest("my-binding", "create");
    when(delegate.invokeBinding(request)).thenReturn(Mono.empty());

    client.invokeBinding(request).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.invoke_binding");
  }

  // -------------------------------------------------------------------------
  // State
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getState(storeName, key, Class) creates span dapr.client.get_state")
  void getStateCreatesSpan() {
    when(delegate.getState("my-store", "my-key", String.class)).thenReturn(Mono.empty());

    client.getState("my-store", "my-key", String.class).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.get_state")
        .that()
        .hasHighCardinalityKeyValue("dapr.store.name", "my-store")
        .hasHighCardinalityKeyValue("dapr.state.key", "my-key");
  }

  @Test
  @DisplayName("saveState creates span dapr.client.save_state")
  void saveStateCreatesSpan() {
    when(delegate.saveState("my-store", "my-key", "value")).thenReturn(Mono.empty());

    client.saveState("my-store", "my-key", "value").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.save_state")
        .that()
        .hasHighCardinalityKeyValue("dapr.store.name", "my-store")
        .hasHighCardinalityKeyValue("dapr.state.key", "my-key");
  }

  @Test
  @DisplayName("deleteState creates span dapr.client.delete_state")
  void deleteStateCreatesSpan() {
    when(delegate.deleteState("my-store", "my-key")).thenReturn(Mono.empty());

    client.deleteState("my-store", "my-key").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.delete_state")
        .that()
        .hasHighCardinalityKeyValue("dapr.store.name", "my-store")
        .hasHighCardinalityKeyValue("dapr.state.key", "my-key");
  }

  @Test
  @DisplayName("getBulkState creates span dapr.client.get_bulk_state")
  void getBulkStateCreatesSpan() {
    when(delegate.getBulkState("my-store", List.of("k1", "k2"), String.class))
        .thenReturn(Mono.empty());

    client.getBulkState("my-store", List.of("k1", "k2"), String.class).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.get_bulk_state");
  }

  @Test
  @DisplayName("executeStateTransaction creates span dapr.client.execute_state_transaction")
  void executeStateTransactionCreatesSpan() {
    when(delegate.executeStateTransaction("my-store", List.of())).thenReturn(Mono.empty());

    client.executeStateTransaction("my-store", List.of()).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.execute_state_transaction");
  }

  // -------------------------------------------------------------------------
  // Secrets
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getSecret(storeName, secretName) creates span dapr.client.get_secret")
  void getSecretCreatesSpan() {
    when(delegate.getSecret("my-vault", "db-password")).thenReturn(Mono.empty());

    client.getSecret("my-vault", "db-password").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.get_secret")
        .that()
        .hasHighCardinalityKeyValue("dapr.secret.store", "my-vault")
        .hasHighCardinalityKeyValue("dapr.secret.name", "db-password");
  }

  @Test
  @DisplayName("getBulkSecret creates span dapr.client.get_bulk_secret")
  void getBulkSecretCreatesSpan() {
    when(delegate.getBulkSecret("my-vault")).thenReturn(Mono.empty());

    client.getBulkSecret("my-vault").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.get_bulk_secret");
  }

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getConfiguration creates span dapr.client.get_configuration")
  void getConfigurationCreatesSpan() {
    when(delegate.getConfiguration("my-config-store", "feature-flag")).thenReturn(Mono.empty());

    client.getConfiguration("my-config-store", "feature-flag").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.get_configuration")
        .that()
        .hasHighCardinalityKeyValue("dapr.configuration.store", "my-config-store");
  }

  @Test
  @DisplayName("subscribeConfiguration creates span dapr.client.subscribe_configuration")
  void subscribeConfigurationCreatesSpan() {
    when(delegate.subscribeConfiguration("my-store", "k1")).thenReturn(Flux.empty());

    client.subscribeConfiguration("my-store", "k1").blockLast();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.subscribe_configuration");
  }

  @Test
  @DisplayName("unsubscribeConfiguration creates span dapr.client.unsubscribe_configuration")
  void unsubscribeConfigurationCreatesSpan() {
    when(delegate.unsubscribeConfiguration("sub-id", "my-store")).thenReturn(Mono.empty());

    client.unsubscribeConfiguration("sub-id", "my-store").block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.unsubscribe_configuration");
  }

  // -------------------------------------------------------------------------
  // Metadata & Lifecycle
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getMetadata creates span dapr.client.get_metadata")
  void getMetadataCreatesSpan() {
    when(delegate.getMetadata()).thenReturn(Mono.empty());

    client.getMetadata().block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.get_metadata");
  }

  @Test
  @DisplayName("waitForSidecar creates span dapr.client.wait_for_sidecar")
  void waitForSidecarCreatesSpan() {
    when(delegate.waitForSidecar(5000)).thenReturn(Mono.empty());

    client.waitForSidecar(5000).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.wait_for_sidecar");
  }

  @Test
  @DisplayName("shutdown creates span dapr.client.shutdown")
  void shutdownCreatesSpan() {
    when(delegate.shutdown()).thenReturn(Mono.empty());

    client.shutdown().block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.shutdown");
  }

  // -------------------------------------------------------------------------
  // Jobs
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("scheduleJob creates span dapr.client.schedule_job")
  void scheduleJobCreatesSpan() {
    ScheduleJobRequest request = new ScheduleJobRequest("nightly-cleanup",
        io.dapr.client.domain.JobSchedule.daily());
    when(delegate.scheduleJob(request)).thenReturn(Mono.empty());

    client.scheduleJob(request).block();

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.client.schedule_job")
        .that()
        .hasHighCardinalityKeyValue("dapr.job.name", "nightly-cleanup");
  }

  // -------------------------------------------------------------------------
  // Deferred start — observation must not leak if Mono is never subscribed
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Observation does not start if the returned Mono is never subscribed")
  void observationDoesNotStartWithoutSubscription() {
    // Call the method but do NOT subscribe (no .block())
    client.publishEvent("pubsub", "topic", "data");

    // Registry must be empty — no observation was started
    TestObservationRegistryAssert.assertThat(registry).doesNotHaveAnyObservation();
  }

  // -------------------------------------------------------------------------
  // Deprecated methods — must NOT create spans
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Deprecated invokeMethod delegates without creating a span")
  @SuppressWarnings("deprecation")
  void deprecatedInvokeMethodDoesNotCreateSpan() {
    when(delegate.invokeMethod(anyString(), anyString(), nullable(Object.class),
        any(io.dapr.client.domain.HttpExtension.class), any(Class.class)))
        .thenReturn(Mono.empty());

    client.invokeMethod("app", "method", (Object) null,
        io.dapr.client.domain.HttpExtension.NONE, String.class).block();

    // Registry must be empty — no spans for deprecated methods
    TestObservationRegistryAssert.assertThat(registry).doesNotHaveAnyObservation();
  }
}
