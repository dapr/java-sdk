package io.dapr.it.testcontainers;

import io.dapr.jobs.client.*;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Random;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        TestDaprJobsConfiguration.class,
        TestJobsApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprJobsIT {

  private static final Network DAPR_NETWORK = Network.newNetwork();
  private static final Random RANDOM = new Random();
  private static final int PORT = RANDOM.nextInt(1000) + 8000;

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer("daprio/daprd:1.15.2")
      .withAppName("jobs-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal")
      .withAppPort(PORT);

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
    registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
    registry.add("server.port", () -> PORT);
  }

  @Autowired
  private DaprJobsClient daprJobsClient;

  @BeforeEach
  public void setUp(){
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
    // Ensure the subscriptions are registered
  }

  @Test
  public void testJobScheduleCreationWithDueTime() {
    OffsetDateTime currentTime = OffsetDateTime.now();
    daprJobsClient.scheduleJob(ScheduleJobRequest.builder().setName("Job").setDueTime(currentTime).build()).block();

    GetJobResponse getJobResponse =
        daprJobsClient.getJob(GetJobRequest.builder().setName("Job").build()).block();
    Assert.assertEquals(currentTime.toString(), getJobResponse.getDueTime().toString());
    Assert.assertEquals("Job", getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithSchedule() {
    OffsetDateTime currentTime = OffsetDateTime.now();
    daprJobsClient.scheduleJob(ScheduleJobRequest.builder().setName("Job")
        .setSchedule(JobSchedule.hourly())
        .setDueTime(currentTime).build()).block();

    GetJobResponse getJobResponse =
        daprJobsClient.getJob(GetJobRequest.builder().setName("Job").build()).block();
    Assert.assertEquals(currentTime.toString(), getJobResponse.getDueTime().toString());
    Assert.assertEquals(JobSchedule.hourly().getExpression(), getJobResponse.getSchedule().getExpression());
    Assert.assertEquals("Job", getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithAllParameters() {
    OffsetDateTime currentTime = OffsetDateTime.now();

    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 2)
        .add(CronPeriod.HOURS, 3)
        .add(DayOfWeek.FRI)
        .build();

    daprJobsClient.scheduleJob(ScheduleJobRequest.builder().setName("Job")
        .setTtl(currentTime.plusHours(2))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setSchedule(JobSchedule.fromString(cronExpression))
        .setDueTime(currentTime).build()).block();

    GetJobResponse getJobResponse =
        daprJobsClient.getJob(GetJobRequest.builder().setName("Job").build()).block();
    Assertions.assertEquals(currentTime.toString(), getJobResponse.getDueTime().toString());
    Assertions.assertEquals("2 * 3 * * FRI", getJobResponse.getSchedule().getExpression());
    Assertions.assertEquals("Job", getJobResponse.getName());
    Assertions.assertEquals(3, getJobResponse.getRepeat());
    Assertions.assertEquals("Job data", new String(getJobResponse.getData()));
    Assertions.assertEquals(currentTime.plusHours(2), getJobResponse.getTtl());
  }

  @Test
  public void testDeleteJobRequest() {
    OffsetDateTime currentTime = OffsetDateTime.now();

    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 2)
        .add(CronPeriod.HOURS, 3)
        .add(DayOfWeek.FRI)
        .build();

    daprJobsClient.scheduleJob(ScheduleJobRequest.builder().setName("Job")
        .setTtl(currentTime.plusHours(2))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setSchedule(JobSchedule.fromString(cronExpression))
        .setDueTime(currentTime).build()).block();

    daprJobsClient.deleteJob(DeleteJobRequest.builder().setName("Job").build()).block();
  }
}
