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

package io.dapr.it.testcontainers.jobs;

import io.dapr.it.testcontainers.TestContainerNetworks;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.DropFailurePolicy;
import io.dapr.client.domain.FailurePolicyType;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.it.testcontainers.DaprClientConfiguration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.Assert.assertEquals;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        DaprClientConfiguration.class,
        TestJobsApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprJobsIT {

  private static final Network DAPR_NETWORK = TestContainerNetworks.GENERAL_NETWORK;
  private static final int PORT = TestContainerNetworks.allocateFreePort();

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
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
  private DaprClient daprClient;

  @BeforeEach
  public void setUp(){
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
  }

  @Test
  public void testJobScheduleCreationWithDueTime() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    Instant currentTime = Instant.now();
    daprClient.scheduleJob(new ScheduleJobRequest("Job", currentTime).setOverwrite(true)).block();

    GetJobResponse getJobResponse =
        daprClient.getJob(new GetJobRequest("Job")).block();

    daprClient.deleteJob(new DeleteJobRequest("Job")).block();

    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals("Job", getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithSchedule() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    Instant currentTime = Instant.now();
    daprClient.scheduleJob(new ScheduleJobRequest("Job", JobSchedule.hourly())
        .setDueTime(currentTime).setOverwrite(true)).block();

    GetJobResponse getJobResponse =
        daprClient.getJob(new GetJobRequest("Job")).block();

    daprClient.deleteJob(new DeleteJobRequest("Job")).block();

    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals(JobSchedule.hourly().getExpression(), getJobResponse.getSchedule().getExpression());
    assertEquals("Job", getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithAllParameters() {
    Instant currentTime = Instant.now();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setOverwrite(true)
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse =
        daprClient.getJob(new GetJobRequest("Job")).block();

    daprClient.deleteJob(new DeleteJobRequest("Job")).block();

    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals("2 * 3 * * FRI", getJobResponse.getSchedule().getExpression());
    assertEquals("Job", getJobResponse.getName());
    assertEquals(Integer.valueOf(3), getJobResponse.getRepeats());
    assertEquals("Job data", new String(getJobResponse.getData()));
    assertEquals(iso8601Formatter.format(currentTime.plus(2, ChronoUnit.HOURS)),
            getJobResponse.getTtl().toString());
  }

  @Test
  public void testJobScheduleCreationWithDropFailurePolicy() {
    Instant currentTime = Instant.now();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC);

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
            .setFailurePolicy(new DropFailurePolicy())
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse =
        daprClient.getJob(new GetJobRequest("Job")).block();

    daprClient.deleteJob(new DeleteJobRequest("Job")).block();

    assertEquals(FailurePolicyType.DROP, getJobResponse.getFailurePolicy().getFailurePolicyType());
  }

  @Test
  public void testJobScheduleCreationWithConstantFailurePolicy() {
    Instant currentTime = Instant.now();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC);

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setFailurePolicy(new ConstantFailurePolicy(3)
            .setDurationBetweenRetries(Duration.of(10, ChronoUnit.SECONDS)))
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse =
        daprClient.getJob(new GetJobRequest("Job")).block();

    daprClient.deleteJob(new DeleteJobRequest("Job")).block();

    ConstantFailurePolicy jobFailurePolicyConstant = (ConstantFailurePolicy) getJobResponse.getFailurePolicy();
    assertEquals(FailurePolicyType.CONSTANT, getJobResponse.getFailurePolicy().getFailurePolicyType());
    assertEquals(3, (int)jobFailurePolicyConstant.getMaxRetries());
    assertEquals(Duration.of(10, ChronoUnit.SECONDS).getNano(),
        jobFailurePolicyConstant.getDurationBetweenRetries().getNano());
  }

  @Test
  public void testDeleteJobRequest() {
    Instant currentTime = Instant.now();

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setOverwrite(true)
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    daprClient.deleteJob(new DeleteJobRequest("Job")).block();
  }
}
