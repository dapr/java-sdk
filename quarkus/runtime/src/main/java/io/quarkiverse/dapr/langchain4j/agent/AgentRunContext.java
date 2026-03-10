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

package io.quarkiverse.dapr.langchain4j.agent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the synchronization state for a single agent execution.
 *
 * <p>When a {@code @Tool}-annotated method is intercepted by {@link DaprToolCallInterceptor},
 * it registers a {@link PendingCall} here and blocks until
 * {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity} executes the
 * tool on the Dapr Workflow Activity thread and completes the future.
 */
public class AgentRunContext {

  /**
   * Holds all the information needed for {@code ToolCallActivity} to execute the tool
   * and unblock the waiting agent thread.
   */
  @SuppressWarnings("EI_EXPOSE_REP")
  public record PendingCall(
      Object target,
      Method method,
      Object[] args,
      CompletableFuture<Object> resultFuture) {

    /**
     * Creates a PendingCall with a defensive copy of args.
     *
     * @param target       the object instance on which the method will be invoked
     * @param method       the reflective method handle
     * @param args         the arguments to pass to the method
     * @param resultFuture future to complete when the call finishes
     */
    public PendingCall(Object target, Method method, Object[] args,
        CompletableFuture<Object> resultFuture) {
      this.target = target;
      this.method = method;
      this.args = args == null ? null : Arrays.copyOf(args, args.length);
      this.resultFuture = resultFuture;
    }

    /**
     * Returns a defensive copy of args.
     *
     * @return copy of args array
     */
    @Override
    public Object[] args() {
      return args == null ? null : Arrays.copyOf(args, args.length);
    }
  }

  private final String agentRunId;
  private final Map<String, PendingCall> pendingCalls = new ConcurrentHashMap<>();

  /**
   * Creates a new AgentRunContext for the given agent run ID.
   *
   * @param agentRunId the unique identifier for the agent run
   */
  public AgentRunContext(String agentRunId) {
    this.agentRunId = agentRunId;
  }

  /**
   * Returns the agent run ID associated with this context.
   *
   * @return the agent run ID
   */
  public String getAgentRunId() {
    return agentRunId;
  }

  /**
   * Register a pending tool call and return the future that will be completed by
   * {@code ToolCallActivity} once the tool has executed.
   *
   * @param toolCallId the unique identifier for this tool call
   * @param target the object instance on which the tool method will be invoked
   * @param method the reflective method handle for the tool
   * @param args the arguments to pass to the tool method
   * @return a future that completes with the tool execution result
   */
  public CompletableFuture<Object> registerCall(
      String toolCallId, Object target, Method method, Object[] args) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    pendingCalls.put(toolCallId, new PendingCall(target, method, args, future));
    return future;
  }

  /**
   * Returns the pending call for the given tool call ID without removing it.
   * Used by {@code ToolCallActivity} to retrieve call details.
   *
   * @param toolCallId the unique identifier for the tool call to look up
   * @return the pending call, or {@code null} if no call exists for the given ID
   */
  public PendingCall getPendingCall(String toolCallId) {
    return pendingCalls.get(toolCallId);
  }

  /**
   * Complete the pending call with a successful result. Removes the entry and
   * unblocks the agent thread waiting in {@link DaprToolCallInterceptor}.
   *
   * @param toolCallId the unique identifier for the tool call to complete
   * @param result the result value to deliver to the waiting thread
   */
  public void completeCall(String toolCallId, Object result) {
    PendingCall call = pendingCalls.remove(toolCallId);
    if (call != null) {
      call.resultFuture().complete(result);
    }
  }

  /**
   * Complete the pending call with an exception. Removes the entry and
   * propagates the failure to the waiting agent thread.
   *
   * @param toolCallId the unique identifier for the tool call to fail
   * @param cause the exception to propagate to the waiting thread
   */
  public void failCall(String toolCallId, Throwable cause) {
    PendingCall call = pendingCalls.remove(toolCallId);
    if (call != null) {
      call.resultFuture().completeExceptionally(cause);
    }
  }
}
