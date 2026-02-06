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

package io.dapr.durabletask;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactories;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Helper class for invoking orchestrations directly, without constructing a {@link DurableTaskGrpcWorker} object.
 *
 * <p>This static class can be used to execute orchestration logic directly. In order to use it for this purpose, the
 * caller must provide orchestration state as serialized protobuf bytes.</p>
 */
public final class OrchestrationRunner {
  private static final Logger logger = Logger.getLogger(OrchestrationRunner.class.getPackage().getName());
  private static final Duration DEFAULT_MAXIMUM_TIMER_INTERVAL = Duration.ofDays(3);

  private OrchestrationRunner() {
  }

  /**
   * Loads orchestration history from {@code base64EncodedOrchestratorRequest} and uses it to execute the
   * orchestrator function code pointed to by {@code orchestratorFunc}.
   *
   * @param base64EncodedOrchestratorRequest the base64-encoded protobuf payload representing an orchestrator execution
   *                                         request
   * @param orchestratorFunc                 a function that implements the orchestrator logic
   * @param <R>                              the type of the orchestrator function output, which must be serializable
   *                                         to JSON
   * @return a base64-encoded protobuf payload of orchestrator actions to be interpreted by the external
   *     orchestration engine
   * @throws IllegalArgumentException if either parameter is {@code null} or
   *                                  if {@code base64EncodedOrchestratorRequest} is not valid base64-encoded protobuf
   */
  public static <R> String loadAndRun(
      String base64EncodedOrchestratorRequest,
      OrchestratorFunction<R> orchestratorFunc) {
    // Example string: CiBhOTMyYjdiYWM5MmI0MDM5YjRkMTYxMDIwNzlmYTM1YSIaCP///////////wESCwi254qRBhDk+rgocgAicgj//////
    // ///8BEgwIs+eKkQYQzMXjnQMaVwoLSGVsbG9DaXRpZXMSACJGCiBhOTMyYjdiYWM5MmI0MDM5YjRkMTYxMDIwNzlmYTM1YRIiCiA3ODEwOTA
    // 2N2Q4Y2Q0ODg1YWU4NjQ0OTNlMmRlMGQ3OA==
    byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedOrchestratorRequest);
    byte[] resultBytes = loadAndRun(decodedBytes, orchestratorFunc);
    return Base64.getEncoder().encodeToString(resultBytes);
  }

  /**
   * Loads orchestration history from {@code orchestratorRequestBytes} and uses it to execute the
   * orchestrator function code pointed to by {@code orchestratorFunc}.
   *
   * @param orchestratorRequestBytes the protobuf payload representing an orchestrator execution request
   * @param orchestratorFunc         a function that implements the orchestrator logic
   * @param <R>                      the type of the orchestrator function output, which must be serializable to JSON
   * @return a protobuf-encoded payload of orchestrator actions to be interpreted by the external orchestration engine
   * @throws IllegalArgumentException if either parameter is {@code null} or if {@code orchestratorRequestBytes} is
   *                                  not valid protobuf
   */
  public static <R> byte[] loadAndRun(
      byte[] orchestratorRequestBytes,
      OrchestratorFunction<R> orchestratorFunc) {
    if (orchestratorFunc == null) {
      throw new IllegalArgumentException("orchestratorFunc must not be null");
    }

    // Wrap the provided lambda in an anonymous TaskOrchestration
    TaskOrchestration orchestration = ctx -> {
      R output = orchestratorFunc.apply(ctx);
      ctx.complete(output);
    };

    return loadAndRun(orchestratorRequestBytes, orchestration);
  }

  /**
   * Loads orchestration history from {@code base64EncodedOrchestratorRequest} and uses it to execute the
   * {@code orchestration}.
   *
   * @param base64EncodedOrchestratorRequest the base64-encoded protobuf payload representing an orchestrator
   *                                         execution request
   * @param orchestration                    the orchestration to execute
   * @return a base64-encoded protobuf payload of orchestrator actions to be interpreted by the external
   *     orchestration engine
   * @throws IllegalArgumentException if either parameter is {@code null} or
   *                                  if {@code base64EncodedOrchestratorRequest} is not valid base64-encoded protobuf
   */
  public static String loadAndRun(
      String base64EncodedOrchestratorRequest,
      TaskOrchestration orchestration) {
    byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedOrchestratorRequest);
    byte[] resultBytes = loadAndRun(decodedBytes, orchestration);
    return Base64.getEncoder().encodeToString(resultBytes);
  }

  /**
   * Loads orchestration history from {@code orchestratorRequestBytes} and uses it to execute the
   * {@code orchestration}.
   *
   * @param orchestratorRequestBytes the protobuf payload representing an orchestrator execution request
   * @param orchestration            the orchestration to execute
   * @return a protobuf-encoded payload of orchestrator actions to be interpreted by the external orchestration engine
   * @throws IllegalArgumentException if either parameter is {@code null} or if {@code orchestratorRequestBytes}
   *                                  is not valid protobuf
   */
  public static byte[] loadAndRun(byte[] orchestratorRequestBytes, TaskOrchestration orchestration) {
    if (orchestratorRequestBytes == null || orchestratorRequestBytes.length == 0) {
      throw new IllegalArgumentException("triggerStateProtoBytes must not be null or empty");
    }

    if (orchestration == null) {
      throw new IllegalArgumentException("orchestration must not be null");
    }

    OrchestratorService.OrchestratorRequest orchestratorRequest;
    try {
      orchestratorRequest = OrchestratorService.OrchestratorRequest.parseFrom(orchestratorRequestBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("triggerStateProtoBytes was not valid protobuf", e);
    }

    // Register the passed orchestration as the default ("*") orchestration
    TaskOrchestrationFactories orchestrationFactories = new TaskOrchestrationFactories();
    orchestrationFactories.addOrchestration(new TaskOrchestrationFactory() {
      @Override
      public String getName() {
        return "*";
      }

      @Override
      public TaskOrchestration create() {
        return orchestration;
      }

      @Override
      public String getVersionName() {
        return "";
      }

      @Override
      public Boolean isLatestVersion() {
        return false;
      }
    });

    TaskOrchestrationExecutor taskOrchestrationExecutor = new TaskOrchestrationExecutor(
        orchestrationFactories,
        new JacksonDataConverter(),
        DEFAULT_MAXIMUM_TIMER_INTERVAL,
        logger,
        null); // No app ID for static runner

    // TODO: Error handling
    TaskOrchestratorResult taskOrchestratorResult = taskOrchestrationExecutor.execute(
        orchestratorRequest.getPastEventsList(),
        orchestratorRequest.getNewEventsList());

    OrchestratorService.OrchestratorResponse response = OrchestratorService.OrchestratorResponse.newBuilder()
        .setInstanceId(orchestratorRequest.getInstanceId())
        .addAllActions(taskOrchestratorResult.getActions())
        .setCustomStatus(StringValue.of(taskOrchestratorResult.getCustomStatus()))
        .build();
    return response.toByteArray();
  }
}
