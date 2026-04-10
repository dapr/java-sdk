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

import io.dapr.config.Properties;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ObservationDaprWorkflowClient}.
 *
 * <p>The gRPC {@code ManagedChannel} created in the parent constructor connects lazily, so
 * instantiation succeeds without a running Dapr sidecar. Actual RPC calls will fail, but the
 * observation lifecycle (start → error → stop) is still validated.
 *
 * <p>Verifies two key requirements:
 * <ol>
 *   <li>Each non-deprecated method creates a correctly named Micrometer Observation span
 *       (even when the underlying call fails due to no sidecar being available).</li>
 *   <li>The wrapper extends {@link DaprWorkflowClient}, so consumers keep injecting
 *       {@code DaprWorkflowClient} without any code changes.</li>
 * </ol>
 */
class ObservationDaprWorkflowClientTest {

  private TestObservationRegistry registry;
  private ObservationDaprWorkflowClient client;

  @BeforeEach
  void setUp() {
    registry = TestObservationRegistry.create();
    // Properties with no gRPC endpoint — channel created lazily, no sidecar needed
    client = new ObservationDaprWorkflowClient(new Properties(), registry);
  }

  // -------------------------------------------------------------------------
  // Transparency — the wrapper IS-A DaprWorkflowClient
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("ObservationDaprWorkflowClient is assignable to DaprWorkflowClient (transparent)")
  void isAssignableToDaprWorkflowClient() {
    assertThat(client).isInstanceOf(DaprWorkflowClient.class);
  }

  // -------------------------------------------------------------------------
  // scheduleNewWorkflow — span is created even when the call fails (no sidecar)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("scheduleNewWorkflow(String) creates span dapr.workflow.schedule")
  void scheduleNewWorkflowByNameCreatesSpan() {
    // The gRPC call will fail — but the span must be recorded with an error
    assertThatThrownBy(() -> client.scheduleNewWorkflow("MyWorkflow"))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.schedule")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.name", "MyWorkflow")
        .hasError();
  }

  @Test
  @DisplayName("scheduleNewWorkflow(Class) delegates to String overload — span is still created")
  void scheduleNewWorkflowByClassDelegatesToStringOverload() {
    // The Class-based overload in the parent calls this.scheduleNewWorkflow(canonicalName),
    // which resolves to our overridden String-based method — the span is created naturally.
    assertThatThrownBy(() -> client.scheduleNewWorkflow(DummyWorkflow.class))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.schedule")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.name", DummyWorkflow.class.getCanonicalName())
        .hasError();
  }

  @Test
  @DisplayName("scheduleNewWorkflow(String, Object, String) includes instance_id in span")
  void scheduleNewWorkflowWithInstanceIdCreatesSpan() {
    assertThatThrownBy(() -> client.scheduleNewWorkflow("MyWorkflow", null, "my-instance-123"))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.schedule")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "my-instance-123")
        .hasError();
  }

  // -------------------------------------------------------------------------
  // Lifecycle operations
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("suspendWorkflow creates span dapr.workflow.suspend")
  void suspendWorkflowCreatesSpan() {
    assertThatThrownBy(() -> client.suspendWorkflow("instance-1", "pausing"))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.suspend")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "instance-1")
        .hasError();
  }

  @Test
  @DisplayName("resumeWorkflow creates span dapr.workflow.resume")
  void resumeWorkflowCreatesSpan() {
    assertThatThrownBy(() -> client.resumeWorkflow("instance-1", "resuming"))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.resume")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "instance-1")
        .hasError();
  }

  @Test
  @DisplayName("terminateWorkflow creates span dapr.workflow.terminate")
  void terminateWorkflowCreatesSpan() {
    assertThatThrownBy(() -> client.terminateWorkflow("instance-1", null))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.terminate")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "instance-1")
        .hasError();
  }

  // -------------------------------------------------------------------------
  // State queries
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("getWorkflowState creates span dapr.workflow.get_state")
  void getWorkflowStateCreatesSpan() {
    assertThatThrownBy(() -> client.getWorkflowState("instance-1", false))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.get_state")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "instance-1")
        .hasError();
  }

  // -------------------------------------------------------------------------
  // Events
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("raiseEvent creates span dapr.workflow.raise_event")
  void raiseEventCreatesSpan() {
    assertThatThrownBy(() -> client.raiseEvent("instance-1", "OrderPlaced", "payload"))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.raise_event")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "instance-1")
        .hasHighCardinalityKeyValue("dapr.workflow.event_name", "OrderPlaced")
        .hasError();
  }

  // -------------------------------------------------------------------------
  // Cleanup
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("purgeWorkflow creates span dapr.workflow.purge")
  void purgeWorkflowCreatesSpan() {
    assertThatThrownBy(() -> client.purgeWorkflow("instance-1"))
        .isInstanceOf(RuntimeException.class);

    TestObservationRegistryAssert.assertThat(registry)
        .hasObservationWithNameEqualTo("dapr.workflow.purge")
        .that()
        .hasHighCardinalityKeyValue("dapr.workflow.instance_id", "instance-1")
        .hasError();
  }

  // -------------------------------------------------------------------------
  // Deprecated methods — must NOT create spans
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Deprecated getInstanceState falls through to parent without creating a span")
  @SuppressWarnings("deprecation")
  void deprecatedGetInstanceStateDoesNotCreateSpan() {
    // This will fail (no sidecar) but must not leave any observations in the registry
    assertThatThrownBy(() -> client.getInstanceState("instance-1", false))
        .isInstanceOf(RuntimeException.class);

    // No spans should have been created for deprecated methods
    TestObservationRegistryAssert.assertThat(registry).doesNotHaveAnyObservation();
  }

  // -------------------------------------------------------------------------
  // Dummy workflow implementation for type-based tests
  // -------------------------------------------------------------------------

  static class DummyWorkflow implements io.dapr.workflows.Workflow {
    @Override
    public io.dapr.workflows.WorkflowStub create() {
      return ctx -> {
      };
    }
  }
}
