package io.dapr.jobs.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DaprJobsClientTest {

  private DaprGrpc.DaprStub daprStub;

  private DaprJobsClient daprJobsClient;

  @BeforeEach
  void setup() {
    ManagedChannel channel = mock(ManagedChannel.class);
    daprStub = mock(DaprGrpc.DaprStub.class);
    when(daprStub.getChannel()).thenReturn(channel);
    when(daprStub.withInterceptors(Mockito.any(), Mockito.any())).thenReturn(daprStub);

    ResiliencyOptions resiliencyOptions = new ResiliencyOptions(); // Default resiliency options
    daprJobsClient = new DaprJobsClient(daprStub, resiliencyOptions);
  }

  @Test
  void scheduleJobShouldSucceedWhenAllFieldsArePresentInRequest() {
    ScheduleJobRequest expectedScheduleJobRequest = new ScheduleJobRequest("testJob",
        JobSchedule.fromString("*/5 * * * *"))
            .setData("testData".getBytes())
            .setTtl(OffsetDateTime.now().plusDays(1))
            .setRepeat(5)
            .setDueTime(OffsetDateTime.now().plusMinutes(10));

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ScheduleJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).scheduleJobAlpha1(any(DaprProtos.ScheduleJobRequest.class), any());

    assertDoesNotThrow(() -> new DaprJobsClient(daprStub, null).scheduleJob(expectedScheduleJobRequest).block());

    ArgumentCaptor<DaprProtos.ScheduleJobRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ScheduleJobRequest.class);

    verify(daprStub, times(1)).scheduleJobAlpha1(captor.capture(), Mockito.any());
    DaprProtos.ScheduleJobRequest actualScheduleJobReq = captor.getValue();

    assertEquals("testJob", actualScheduleJobReq.getJob().getName());
    assertEquals("testData",
            new String(actualScheduleJobReq.getJob().getData().getValue().toByteArray(), StandardCharsets.UTF_8));
    assertEquals("*/5 * * * *", actualScheduleJobReq.getJob().getSchedule());
    assertEquals(expectedScheduleJobRequest.getTtl().toString(), actualScheduleJobReq.getJob().getTtl());
    assertEquals(expectedScheduleJobRequest.getRepeats(), actualScheduleJobReq.getJob().getRepeats());
    assertEquals(expectedScheduleJobRequest.getDueTime().toString(), actualScheduleJobReq.getJob().getDueTime());
  }

  @Test
  void scheduleJobShouldSucceedWhenRequiredFieldsNameAndDueTimeArePresentInRequest() {

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ScheduleJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).scheduleJobAlpha1(any(DaprProtos.ScheduleJobRequest.class), any());

    ScheduleJobRequest expectedScheduleJobRequest =
        new ScheduleJobRequest("testJob", OffsetDateTime.now().plusMinutes(10));
    assertDoesNotThrow(() -> daprJobsClient.scheduleJob(expectedScheduleJobRequest).block());

    ArgumentCaptor<DaprProtos.ScheduleJobRequest> captor =
            ArgumentCaptor.forClass(DaprProtos.ScheduleJobRequest.class);

    verify(daprStub, times(1)).scheduleJobAlpha1(captor.capture(), Mockito.any());
    DaprProtos.ScheduleJobRequest actualScheduleJobRequest = captor.getValue();
    DaprProtos.Job job = actualScheduleJobRequest.getJob();
    assertEquals("testJob", job.getName());
    assertFalse(job.hasData());
    assertFalse(job.hasSchedule());
    assertEquals(0, job.getRepeats());
    assertFalse(job.hasTtl());
    assertEquals(job.getDueTime(), actualScheduleJobRequest.getJob().getDueTime());
  }

  @Test
  void scheduleJobShouldSucceedWhenRequiredFieldsNameAndScheduleArePresentInRequest() {

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.ScheduleJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).scheduleJobAlpha1(any(DaprProtos.ScheduleJobRequest.class), any());

    ScheduleJobRequest expectedScheduleJobRequest = new ScheduleJobRequest("testJob",
        JobSchedule.fromString("* * * * * *"));
    assertDoesNotThrow(() -> daprJobsClient.scheduleJob(expectedScheduleJobRequest).block());

    ArgumentCaptor<DaprProtos.ScheduleJobRequest> captor =
        ArgumentCaptor.forClass(DaprProtos.ScheduleJobRequest.class);

    verify(daprStub, times(1)).scheduleJobAlpha1(captor.capture(), Mockito.any());
    DaprProtos.ScheduleJobRequest actualScheduleJobRequest = captor.getValue();
    DaprProtos.Job job = actualScheduleJobRequest.getJob();
    assertEquals("testJob", job.getName());
    assertFalse(job.hasData());
    assertEquals( "* * * * * *", job.getSchedule());
    assertEquals(0, job.getRepeats());
    assertFalse(job.hasTtl());
    assertEquals(job.getDueTime(), actualScheduleJobRequest.getJob().getDueTime());
  }

  @Test
  void scheduleJobShouldThrowWhenRequestIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.scheduleJob(null).block();
    });
    assertEquals("scheduleJobRequest cannot be null", exception.getMessage());
  }

  @Test
  void scheduleJobShouldThrowWhenInvalidRequest() {
    ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest(null, OffsetDateTime.now());
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.scheduleJob(scheduleJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  void scheduleJobShouldThrowWhenNameInRequestIsEmpty() {
    ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest("", OffsetDateTime.now());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.scheduleJob(scheduleJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  void getJobShouldReturnResponseWhenAllFieldsArePresentInRequest() {
    GetJobRequest getJobRequest = new GetJobRequest("testJob");

    DaprProtos.Job job = DaprProtos.Job.newBuilder()
            .setName("testJob")
            .setTtl(OffsetDateTime.now().toString())
            .setData(Any.newBuilder().setValue(ByteString.copyFrom("testData".getBytes())).build())
            .setSchedule("*/5 * * * *")
            .setRepeats(5)
            .setDueTime(OffsetDateTime.now().plusMinutes(10).toString())
            .build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.GetJobResponse> observer = invocation.getArgument(1);
      observer.onNext(DaprProtos.GetJobResponse.newBuilder()
              .setJob(job)
              .build());
      observer.onCompleted();
      return null;
    }).when(daprStub).getJobAlpha1(any(DaprProtos.GetJobRequest.class), any());

    Mono<GetJobResponse> resultMono = daprJobsClient.getJob(getJobRequest);

    GetJobResponse response = resultMono.block();
    assertNotNull(response);
    assertEquals("testJob", response.getName());
    assertEquals("testData", new String(response.getData(), StandardCharsets.UTF_8));
    assertEquals("*/5 * * * *", response.getSchedule().getExpression());
    assertEquals(5, response.getRepeats());
    assertEquals(job.getTtl(), response.getTtl().toString());
    assertEquals(job.getDueTime(), response.getDueTime().toString());
  }

  @Test
  void getJobShouldReturnResponseWhenRequiredFieldsArePresentInRequest() {
    GetJobRequest getJobRequest = new GetJobRequest("testJob");

    DaprProtos.Job job = DaprProtos.Job.newBuilder()
            .setName("testJob")
            .build();

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.GetJobResponse> observer = invocation.getArgument(1);
      observer.onNext(DaprProtos.GetJobResponse.newBuilder()
              .setJob(job)
              .build());
      observer.onCompleted();
      return null;
    }).when(daprStub).getJobAlpha1(any(DaprProtos.GetJobRequest.class), any());

    Mono<GetJobResponse> resultMono = daprJobsClient.getJob(getJobRequest);

    GetJobResponse response = resultMono.block();
    assertNotNull(response);
    assertEquals("testJob", response.getName());
    assertNull(response.getData());
    assertNull(response.getSchedule());
    assertNull(response.getRepeats());
    assertNull(response.getTtl());
    assertNull(response.getDueTime());
  }

  @Test
  void getJobShouldThrowWhenRequestIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.getJob(null).block();
    });
    assertEquals("getJobRequest cannot be null", exception.getMessage());
  }

  @Test
  void getJobShouldThrowWhenNameIsNullRequest() {
    GetJobRequest getJobRequest = new GetJobRequest(null);

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.getJob(getJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  void getJobShouldThrowWhenNameIsEmptyRequest() {
    GetJobRequest getJobRequest =new GetJobRequest("");;

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.getJob(getJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  void deleteJobShouldSucceedWhenValidRequest() {
    DeleteJobRequest deleteJobRequest = new DeleteJobRequest("testJob");

    doAnswer(invocation -> {
      StreamObserver<DaprProtos.DeleteJobResponse> observer = invocation.getArgument(1);
      observer.onCompleted(); // Simulate successful response
      return null;
    }).when(daprStub).deleteJobAlpha1(any(DaprProtos.DeleteJobRequest.class), any());

    Mono<Void> resultMono = daprJobsClient.deleteJob(deleteJobRequest);

    assertDoesNotThrow(() -> resultMono.block());
  }

  @Test
  void deleteJobShouldThrowRequestIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.deleteJob(null).block();
    });
    assertEquals("deleteJobRequest cannot be null", exception.getMessage());
  }

  @Test
  void deleteJobShouldThrowWhenNameIsNullRequest() {
    DeleteJobRequest deleteJobRequest = new DeleteJobRequest(null);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.deleteJob(deleteJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  void deleteJobShouldThrowWhenNameIsEmptyRequest() {
    DeleteJobRequest deleteJobRequest = new DeleteJobRequest("");
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      daprJobsClient.deleteJob(deleteJobRequest).block();
    });
    assertEquals("Name in the request cannot be null or empty", exception.getMessage());
  }

  @Test
  void closeShouldCloseChannel() throws Exception {
    ManagedChannel mockChannel = mock(ManagedChannel.class);
    when(daprStub.getChannel()).thenReturn(mockChannel);

    when(mockChannel.isShutdown()).thenReturn(Boolean.valueOf(false));

    daprJobsClient.close();

    verify(mockChannel, times(1)).shutdown();
  }
}