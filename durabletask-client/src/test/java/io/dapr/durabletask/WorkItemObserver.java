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

import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts how the sidecar delivers workflow history over the wire.
 *
 * <p>Mirrors the Go equivalent in dapr's integration framework
 * ({@code tests/integration/framework/process/workflow/worker.go}) and the Python SDK's observer:
 * every {@code WorkflowRequest} arrives either as a delta ({@code cachedHistory} set, so
 * {@code pastEvents} carries only the events since the worker was last brought up to date) or as a
 * full send, and every {@code GetInstanceHistory} call is a cache miss the worker had to recover
 * from.
 *
 * <p>Without this, an end-to-end test cannot tell a working delta path from a sidecar that ignored
 * {@code WORKER_CAPABILITY_STATEFUL_HISTORY} altogether: both produce identical workflow output.
 *
 * <p>gRPC delivers messages on channel executor threads while the test asserts from the JUnit
 * thread, hence the concurrent counters.
 */
final class WorkItemObserver implements ClientInterceptor {

  private static final String GET_WORK_ITEMS = "TaskHubSidecarService/GetWorkItems";
  private static final String GET_INSTANCE_HISTORY = "TaskHubSidecarService/GetInstanceHistory";

  private final Map<String, AtomicInteger> deltas = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> fullSends = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> historyFetches = new ConcurrentHashMap<>();

  /** Work items for this instance whose committed-history prefix the sidecar omitted. */
  int deltas(String instanceId) {
    return count(this.deltas, instanceId);
  }

  /** Work items for this instance carrying the full committed history. */
  int fullSends(String instanceId) {
    return count(this.fullSends, instanceId);
  }

  /** GetInstanceHistory calls for this instance, i.e. cache misses the worker recovered from. */
  int historyFetches(String instanceId) {
    return count(this.historyFetches, instanceId);
  }

  private static int count(Map<String, AtomicInteger> counters, String instanceId) {
    AtomicInteger counter = counters.get(instanceId);
    return counter == null ? 0 : counter.get();
  }

  private static void increment(Map<String, AtomicInteger> counters, String instanceId) {
    counters.computeIfAbsent(instanceId, key -> new AtomicInteger()).incrementAndGet();
  }

  private void recordWorkItem(OrchestratorService.WorkItem workItem) {
    if (!workItem.hasWorkflowRequest()) {
      return;
    }
    OrchestratorService.WorkflowRequest request = workItem.getWorkflowRequest();
    if (request.hasCachedHistory()) {
      increment(this.deltas, request.getInstanceId());
    } else {
      increment(this.fullSends, request.getInstanceId());
    }
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

    String fullMethodName = method.getFullMethodName();

    if (fullMethodName.endsWith(GET_INSTANCE_HISTORY)) {
      return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
          next.newCall(method, callOptions)) {
        @Override
        public void sendMessage(ReqT message) {
          if (message instanceof OrchestratorService.GetInstanceHistoryRequest) {
            increment(historyFetches, ((OrchestratorService.GetInstanceHistoryRequest) message).getInstanceId());
          }
          super.sendMessage(message);
        }
      };
    }

    if (!fullMethodName.endsWith(GET_WORK_ITEMS)) {
      return next.newCall(method, callOptions);
    }

    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        super.start(
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
              @Override
              public void onMessage(RespT message) {
                if (message instanceof OrchestratorService.WorkItem) {
                  recordWorkItem((OrchestratorService.WorkItem) message);
                }
                super.onMessage(message);
              }
            },
            headers);
      }
    };
  }
}
