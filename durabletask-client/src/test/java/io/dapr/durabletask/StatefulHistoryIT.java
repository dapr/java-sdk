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
 * limitations under the License.
 */

package io.dapr.durabletask;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-level verification that the sidecar really delivers history deltas.
 *
 * <p>Requires a sidecar implementing the stateful-history protocol (dapr/durabletask-go#110). CI's
 * {@code build-durabletask} job builds dapr/durabletask-go from its default branch, which contains
 * it. Against an older sidecar the capability is ignored and every turn arrives as a full send,
 * which is exactly what {@link #deltaDeliveryReducesFullSends()} is written to catch.
 *
 * <p>Asserting on workflow output alone would prove nothing here: a correct delta path and a
 * sidecar that never sends deltas produce identical results. The counts come from a gRPC
 * interceptor watching the real work-item stream.
 */
@Tag("integration")
class StatefulHistoryIT extends IntegrationTestBase {

  private static final int TURNS = 20;
  private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(60);

  /**
   * The sidecar records how much history a stream holds only <em>after</em> rewriting a work item,
   * so the first turn (empty past) leaves the watermark at zero and the second still fails the
   * "worker holds something" check. Deltas therefore start at the third turn. dapr's largehistory
   * integration test asserts the same bound.
   */
  private static final int MAX_WARMUP_FULL_SENDS = 2;

  /** Counts of how one run's work items were delivered, plus the value the workflow returned. */
  private static final class RunResult {
    final int deltas;
    final int fullSends;
    final int historyFetches;
    final int output;

    RunResult(int deltas, int fullSends, int historyFetches, int output) {
      this.deltas = deltas;
      this.fullSends = fullSends;
      this.historyFetches = historyFetches;
      this.output = output;
    }

    @Override
    public String toString() {
      return String.format("deltas=%d, fullSends=%d, historyFetches=%d, output=%d",
          this.deltas, this.fullSends, this.historyFetches, this.output);
    }
  }

  /**
   * Runs a long sequential activity chain, so each activity result is its own turn and the
   * committed history grows every turn. That is what makes the omitted prefix, and therefore the
   * delta, large enough to be worth measuring.
   *
   * <p>Each run gets a fresh worker and channel, hence a fresh work-item stream, so the sidecar's
   * warm set starts empty and the counts describe this run alone.
   */
  private RunResult runAccumulate(boolean disableStatefulHistory) throws TimeoutException {
    final String orchestratorName = "StatefulHistoryAccumulate";
    final String activityName = "PlusOne";

    WorkItemObserver observer = new WorkItemObserver();
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("127.0.0.1", 4001)
        .usePlaintext()
        .intercept(observer)
        .build();

    try {
      DurableTaskGrpcWorker worker = this.createWorkerBuilder()
          .grpcChannel(channel)
          .disableStatefulHistory(disableStatefulHistory)
          .addOrchestrator(orchestratorName, ctx -> {
            int current = ctx.getInput(Integer.class);
            for (int i = 0; i < TURNS; i++) {
              current = ctx.callActivity(activityName, current, Integer.class).await();
            }
            ctx.complete(current);
          })
          .addActivity(activityName, ctx -> ctx.getInput(Integer.class) + 1)
          .buildAndStart();

      DurableTaskClient client = new DurableTaskGrpcClientBuilder().build();
      try (worker; client) {
        String instanceId = client.scheduleNewOrchestrationInstance(orchestratorName, 0);
        OrchestrationMetadata instance = client.waitForInstanceCompletion(instanceId, COMPLETION_TIMEOUT, true);

        assertNotNull(instance);
        assertEquals(OrchestrationRuntimeStatus.COMPLETED, instance.getRuntimeStatus());

        return new RunResult(
            observer.deltas(instanceId),
            observer.fullSends(instanceId),
            observer.historyFetches(instanceId),
            instance.readOutputAs(Integer.class));
      }
    } finally {
      channel.shutdownNow();
      try {
        channel.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Test
  void deltaDeliveryReducesFullSends() throws TimeoutException {
    RunResult result = runAccumulate(false);

    assertEquals(TURNS, result.output, () -> "workflow produced the wrong result: " + result);
    assertTrue(result.deltas > 0, () -> "sidecar never sent a delta: " + result);
    assertTrue(result.fullSends <= MAX_WARMUP_FULL_SENDS, () -> "too many full sends: " + result);
    assertTrue(result.deltas >= TURNS - MAX_WARMUP_FULL_SENDS,
        () -> "expected a delta for nearly every turn: " + result);
  }

  @Test
  void warmStreamNeverMissesItsCache() throws TimeoutException {
    RunResult result = runAccumulate(false);

    assertEquals(TURNS, result.output, () -> "workflow produced the wrong result: " + result);
    assertEquals(0, result.historyFetches, () -> "unexpected GetInstanceHistory recovery: " + result);
  }

  @Test
  void disabledWorkerReceivesOnlyFullHistories() throws TimeoutException {
    RunResult result = runAccumulate(true);

    assertEquals(TURNS, result.output, () -> "workflow produced the wrong result: " + result);
    assertEquals(0, result.deltas,
        () -> "delta sent to a worker that never advertised support: " + result);
    assertTrue(result.fullSends >= TURNS, () -> "expected a full send per turn: " + result);
  }
}
