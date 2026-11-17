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

package io.dapr.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.DropFailurePolicy;
import io.dapr.client.domain.FailurePolicy;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.FailurePolicyUtils;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprJobsProtos;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Jobs API calls of {@link DaprClientImpl}: schedule, get and delete, with fallback to the Alpha1 RPCs.
 */
final class DaprJobsClient {

  private final DaprGrpcCalls grpcCalls;

  DaprJobsClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Void> scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    try {
      validateScheduleJobRequest(scheduleJobRequest);

      DaprJobsProtos.Job.Builder jobBuilder = DaprJobsProtos.Job.newBuilder();
      jobBuilder.setName(scheduleJobRequest.getName());

      DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
              .withZone(ZoneOffset.UTC);

      if (scheduleJobRequest.getData() != null) {
        jobBuilder.setData(Any.newBuilder()
            .setValue(ByteString.copyFrom(scheduleJobRequest.getData())).build());
      }

      if (scheduleJobRequest.getSchedule() != null) {
        jobBuilder.setSchedule(scheduleJobRequest.getSchedule().getExpression());
      }

      if (scheduleJobRequest.getTtl() != null) {
        jobBuilder.setTtl(iso8601Formatter.format(scheduleJobRequest.getTtl()));
      }

      if (scheduleJobRequest.getRepeats() != null) {
        jobBuilder.setRepeats(scheduleJobRequest.getRepeats());
      }

      if (scheduleJobRequest.getDueTime() != null) {
        jobBuilder.setDueTime(iso8601Formatter.format(scheduleJobRequest.getDueTime()));
      }

      if (scheduleJobRequest.getFailurePolicy() != null) {
        jobBuilder.setFailurePolicy(FailurePolicyUtils.getJobFailurePolicy(scheduleJobRequest.getFailurePolicy()));
      }

      DaprJobsProtos.ScheduleJobRequest request = DaprJobsProtos.ScheduleJobRequest.newBuilder()
          .setOverwrite(scheduleJobRequest.getOverwrite())
          .setJob(jobBuilder.build())
          .build();

      Mono<DaprJobsProtos.ScheduleJobResponse> scheduleJobResponseMono =
          Mono.deferContextual(context ->
              grpcCalls.<DaprJobsProtos.ScheduleJobResponse>createMono(
                  it -> grpcCalls.intercept(context).scheduleJob(request, it)
              ).onErrorResume(throwable -> {
                // The stable ScheduleJob RPC is unavailable on pre-1.18 sidecars, fall back to Alpha1.
                if (isUnimplemented(throwable)) {
                  return grpcCalls.<DaprJobsProtos.ScheduleJobResponse>createMono(
                      it -> grpcCalls.intercept(context).scheduleJobAlpha1(request, it));
                }
                return Mono.error(throwable);
              })
          );

      return scheduleJobResponseMono.then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<GetJobResponse> getJob(GetJobRequest getJobRequest) {
    try {
      validateGetJobRequest(getJobRequest);

      DaprJobsProtos.GetJobRequest request = DaprJobsProtos.GetJobRequest.newBuilder()
          .setName(getJobRequest.getName())
          .build();

      Mono<DaprJobsProtos.GetJobResponse> getJobResponseMono =
          Mono.deferContextual(context ->
              grpcCalls.<DaprJobsProtos.GetJobResponse>createMono(
                  it -> grpcCalls.intercept(context).getJob(request, it)
              ).onErrorResume(throwable -> {
                // The stable GetJob RPC is unavailable on pre-1.18 sidecars, fall back to Alpha1.
                if (isUnimplemented(throwable)) {
                  return grpcCalls.<DaprJobsProtos.GetJobResponse>createMono(
                      it -> grpcCalls.intercept(context).getJobAlpha1(request, it));
                }
                return Mono.error(throwable);
              })
          );

      return getJobResponseMono.map(response -> {
        DaprJobsProtos.Job job = response.getJob();
        GetJobResponse getJobResponse = null;

        if (job.hasSchedule() && job.hasDueTime()) {
          getJobResponse = new GetJobResponse(job.getName(), JobSchedule.fromString(job.getSchedule()));
          getJobResponse.setDueTime(Instant.parse(job.getDueTime()));
        } else if (job.hasSchedule()) {
          getJobResponse = new GetJobResponse(job.getName(), JobSchedule.fromString(job.getSchedule()));
        } else {
          getJobResponse = new GetJobResponse(job.getName(), Instant.parse(job.getDueTime()));
        }

        if (job.hasFailurePolicy()) {
          getJobResponse.setFailurePolicy(getJobFailurePolicy(job.getFailurePolicy()));
        }

        return getJobResponse
            .setTtl(job.hasTtl() ? Instant.parse(job.getTtl()) : null)
            .setData(job.hasData() ? job.getData().getValue().toByteArray() : null)
            .setRepeat(job.hasRepeats() ? job.getRepeats() : null);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private FailurePolicy getJobFailurePolicy(CommonProtos.JobFailurePolicy jobFailurePolicy) {
    if (jobFailurePolicy.hasDrop()) {
      return new DropFailurePolicy();
    }

    CommonProtos.JobFailurePolicyConstant jobFailurePolicyConstant = jobFailurePolicy.getConstant();
    if (jobFailurePolicyConstant.hasInterval() && jobFailurePolicyConstant.hasMaxRetries()) {
      return new ConstantFailurePolicy(jobFailurePolicyConstant.getMaxRetries())
              .setDurationBetweenRetries(Duration.of(jobFailurePolicyConstant.getInterval().getNanos(),
                  ChronoUnit.NANOS));
    }

    if (jobFailurePolicyConstant.hasMaxRetries()) {
      return new ConstantFailurePolicy(jobFailurePolicyConstant.getMaxRetries());
    }

    return new ConstantFailurePolicy(
        Duration.of(jobFailurePolicyConstant.getInterval().getNanos(),
            ChronoUnit.NANOS));
  }

  Mono<Void> deleteJob(DeleteJobRequest deleteJobRequest) {
    try {
      validateDeleteJobRequest(deleteJobRequest);

      DaprJobsProtos.DeleteJobRequest request = DaprJobsProtos.DeleteJobRequest.newBuilder()
          .setName(deleteJobRequest.getName())
          .build();

      Mono<DaprJobsProtos.DeleteJobResponse> deleteJobResponseMono =
          Mono.deferContextual(context ->
              grpcCalls.<DaprJobsProtos.DeleteJobResponse>createMono(
                  it -> grpcCalls.intercept(context).deleteJob(request, it)
              ).onErrorResume(throwable -> {
                // The stable DeleteJob RPC is unavailable on pre-1.18 sidecars, fall back to Alpha1.
                if (isUnimplemented(throwable)) {
                  return grpcCalls.<DaprJobsProtos.DeleteJobResponse>createMono(
                      it -> grpcCalls.intercept(context).deleteJobAlpha1(request, it));
                }
                return Mono.error(throwable);
              })
          );

      return deleteJobResponseMono.then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Returns true when a failed RPC indicates the sidecar has not implemented the stable RPC,
   * so the caller should retry against the deprecated Alpha1 RPC. The gRPC status is wrapped in a
   * {@link DaprException} (and an {@link java.util.concurrent.ExecutionException}), so we let gRPC
   * walk the cause chain to recover the original status code.
   */
  private static boolean isUnimplemented(Throwable throwable) {
    return io.grpc.Status.fromThrowable(throwable).getCode() == io.grpc.Status.Code.UNIMPLEMENTED;
  }

  private void validateScheduleJobRequest(ScheduleJobRequest scheduleJobRequest) {
    if (scheduleJobRequest == null) {
      throw new IllegalArgumentException("scheduleJobRequest cannot be null");
    }

    if (scheduleJobRequest.getName() == null || scheduleJobRequest.getName().isEmpty()) {
      throw new IllegalArgumentException("Name in the request cannot be null or empty");
    }

    if (scheduleJobRequest.getSchedule() == null && scheduleJobRequest.getDueTime() == null) {
      throw new IllegalArgumentException("At least one of schedule or dueTime must be provided");
    }
  }

  private void validateGetJobRequest(GetJobRequest getJobRequest) {
    if (getJobRequest == null) {
      throw new IllegalArgumentException("getJobRequest cannot be null");
    }

    if (getJobRequest.getName() == null || getJobRequest.getName().isEmpty()) {
      throw new IllegalArgumentException("Name in the request cannot be null or empty");
    }
  }

  private void validateDeleteJobRequest(DeleteJobRequest deleteJobRequest) {
    if (deleteJobRequest == null) {
      throw new IllegalArgumentException("deleteJobRequest cannot be null");
    }

    if (deleteJobRequest.getName() == null || deleteJobRequest.getName().isEmpty()) {
      throw new IllegalArgumentException("Name in the request cannot be null or empty");
    }
  }
}
