package io.dapr.jobs.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.exceptions.DaprHttpException;
import io.dapr.internal.grpc.interceptors.DaprTimeoutInterceptor;
import io.dapr.internal.grpc.interceptors.DaprTracingInterceptor;
import io.dapr.internal.resiliency.RetryPolicy;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.dapr.utils.NetworkUtils;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.ContextView;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

public class DaprJobsClient implements AutoCloseable {

  /**
   * Stub that has the method to call the conversation apis.
   */
  private final DaprGrpc.DaprStub asyncStub;

  /**
   * The retry policy.
   */
  private final RetryPolicy retryPolicy;

  /**
   * The timeout policy.
   */
  private final TimeoutPolicy timeoutPolicy;

  /**
   * Constructor to create conversation client.
   */
  public DaprJobsClient() {
    this(DaprGrpc.newStub(NetworkUtils.buildGrpcManagedChannel(new Properties())), null);
  }

  /**
   * Constructor.
   *
   * @param properties with client configuration options.
   * @param resiliencyOptions retry options.
   */
  public DaprJobsClient(
      Properties properties,
      ResiliencyOptions resiliencyOptions) {
    this(DaprGrpc.newStub(NetworkUtils.buildGrpcManagedChannel(properties)), resiliencyOptions);
  }

  /**
   * ConversationClient constructor.
   *
   * @param resiliencyOptions timeout and retry policies.
   */
  protected DaprJobsClient(
      DaprGrpc.DaprStub asyncStub,
      ResiliencyOptions resiliencyOptions) {
    this.asyncStub = asyncStub;
    this.retryPolicy = new RetryPolicy(resiliencyOptions == null ? null : resiliencyOptions.getMaxRetries());
    this.timeoutPolicy = new TimeoutPolicy(resiliencyOptions == null ? null : resiliencyOptions.getTimeout());
  }

  /**
   * Schedules a job using the provided job request details.
   *
   * @param createJobRequest The request containing the details of the job to schedule.
   *                         Must include a name and optional schedule, data, and other related properties.
   * @return A {@link Mono} that completes when the job scheduling operation is successful or raises an error.
   * @throws IllegalArgumentException If the request or its required fields like name are null or empty.
   */
  public Mono<Void> scheduleJob(ScheduleJobRequest createJobRequest) {
    try {
      if (createJobRequest == null) {
        throw new IllegalArgumentException("scheduleJobRequest cannot be null");
      }

      if (createJobRequest.getName() == null || createJobRequest.getName().isEmpty()) {
        throw new IllegalArgumentException("Name in the request cannot be null or empty");
      }

      if (createJobRequest.getSchedule() == null && createJobRequest.getDueTime() == null) {
        throw new IllegalArgumentException("At least one of schedule or dueTime must be provided");
      }

      DaprProtos.Job.Builder scheduleJobRequestBuilder = DaprProtos.Job.newBuilder();
      scheduleJobRequestBuilder.setName(createJobRequest.getName());

      if (createJobRequest.getData() != null) {
        scheduleJobRequestBuilder.setData(Any.newBuilder()
                .setValue(ByteString.copyFrom(createJobRequest.getData())).build());
      }

      if (createJobRequest.getSchedule() != null) {
        scheduleJobRequestBuilder.setSchedule(createJobRequest.getSchedule().getExpression());
      }

      if (createJobRequest.getTtl() != null) {
        scheduleJobRequestBuilder.setTtl(createJobRequest.getTtl().toString());
      }

      if (createJobRequest.getRepeats() != null) {
        scheduleJobRequestBuilder.setRepeats(createJobRequest.getRepeats());
      }

      if (createJobRequest.getDueTime() != null) {
        scheduleJobRequestBuilder.setDueTime(createJobRequest.getDueTime().toString());
      }

      DaprProtos.ScheduleJobRequest scheduleJobRequest = DaprProtos.ScheduleJobRequest.newBuilder()
              .setJob(scheduleJobRequestBuilder.build()).build();

      Mono<DaprProtos.ScheduleJobResponse> scheduleJobResponseMono =
              Mono.deferContextual(context -> this.createMono(
                              it -> intercept(context, asyncStub)
                                      .scheduleJobAlpha1(scheduleJobRequest, it)
                      )
              );

      return scheduleJobResponseMono.then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Retrieves details of a specific job.
   *
   * @param getJobRequest The request containing the job name for which the details are to be fetched.
   *      The name property is mandatory.
   * @return A {@link Mono} that emits the {@link GetJobResponse} containing job details or raises an
   *      error if the job is not found.
   * @throws IllegalArgumentException If the request or its required fields like name are null or empty.
   */

  public Mono<GetJobResponse> getJob(GetJobRequest getJobRequest) {
    try {
      if (getJobRequest == null) {
        throw new IllegalArgumentException("getJobRequest cannot be null");
      }

      if (getJobRequest.getName() == null || getJobRequest.getName().isEmpty()) {
        throw new IllegalArgumentException("Name in the request cannot be null or empty");
      }

      Mono<DaprProtos.GetJobResponse> getJobResponseMono =
          Mono.deferContextual(context -> this.createMono(
                  it -> intercept(context, asyncStub)
                      .getJobAlpha1(DaprProtos.GetJobRequest.newBuilder()
                          .setName(getJobRequest.getName()).build(), it)
              )
          );

      return getJobResponseMono.map(response -> {
        DaprProtos.Job job = response.getJob();
        GetJobResponse getJobResponse = null;
        if (job.hasSchedule()) {
          getJobResponse = new GetJobResponse(job.getName(), JobSchedule.fromString(job.getSchedule()));
        } else {
          getJobResponse = new GetJobResponse(job.getName(), OffsetDateTime.parse(job.getDueTime()));
        }

        return getJobResponse
            .setTtl(job.hasTtl() ? OffsetDateTime.parse(job.getTtl()) : null)
            .setData(job.hasData() ? job.getData().getValue().toByteArray() : null)
            .setRepeat(job.hasRepeats() ? job.getRepeats() : null);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Deletes a job based on the given request.
   *
   * @param deleteJobRequest The request containing the job name to be deleted.
   *                        The name property is mandatory.
   * @return A {@link Mono} that completes when the job is successfully deleted or raises an error.
   * @throws IllegalArgumentException If the request or its required fields like name are null or empty.
   */
  public Mono<Void> deleteJob(DeleteJobRequest deleteJobRequest) {
    try {
      if (deleteJobRequest == null) {
        throw new IllegalArgumentException("deleteJobRequest cannot be null");
      }

      if (deleteJobRequest.getName() == null || deleteJobRequest.getName().isEmpty()) {
        throw new IllegalArgumentException("Name in the request cannot be null or empty");
      }

      Mono<DaprProtos.DeleteJobResponse> deleteJobResponseMono =
              Mono.deferContextual(context -> this.createMono(
                              it -> intercept(context, asyncStub)
                                      .deleteJobAlpha1(DaprProtos.DeleteJobRequest.newBuilder()
                                              .setName(deleteJobRequest.getName()).build(), it)
                      )
              );

      return deleteJobResponseMono.then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  @Override
  public void close() throws Exception {
    ManagedChannel channel = (ManagedChannel) this.asyncStub.getChannel();

    DaprException.wrap(() -> {
      if (channel != null && !channel.isShutdown()) {
        channel.shutdown();
      }

      return true;
    }).call();
  }

  private DaprGrpc.DaprStub intercept(
      ContextView context, DaprGrpc.DaprStub client) {
    return client.withInterceptors(new DaprTimeoutInterceptor(this.timeoutPolicy), new DaprTracingInterceptor(context));
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Mono.create(sink -> DaprException.wrap(() -> consumer.accept(
            createStreamObserver(sink))).run()));
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(DaprHttpException.fromGrpcExecutionException(null, t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }
}
