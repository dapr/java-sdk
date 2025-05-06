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

package io.dapr.it.testcontainers;

import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.ScheduleJobRequest;
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.Assert.assertEquals;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        DaprPreviewClientConfiguration.class,
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
  private DaprPreviewClient daprPreviewClient;

  @BeforeEach
  public void setUp(){
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
  }

  @Test
  public void testJobScheduleCreationWithDueTime() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    Instant currentTime = Instant.now();
    daprPreviewClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)).block();

    GetJobResponse getJobResponse =
        daprPreviewClient.getJob(new GetJobRequest("Job")).block();
    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals("Job", getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithSchedule() {
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    Instant currentTime = Instant.now();
    daprPreviewClient.scheduleJob(new ScheduleJobRequest("Job", JobSchedule.hourly())
        .setDueTime(currentTime)).block();

    GetJobResponse getJobResponse =
        daprPreviewClient.getJob(new GetJobRequest("Job")).block();
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

    daprPreviewClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse =
        daprPreviewClient.getJob(new GetJobRequest("Job")).block();
    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals("2 * 3 * * FRI", getJobResponse.getSchedule().getExpression());
    assertEquals("Job", getJobResponse.getName());
    assertEquals(Integer.valueOf(3), getJobResponse.getRepeats());
    assertEquals("Job data", new String(getJobResponse.getData()));
    assertEquals(iso8601Formatter.format(currentTime.plus(2, ChronoUnit.HOURS)),
            getJobResponse.getTtl().toString());
  }

  @Test
  public void testDeleteJobRequest() {
    Instant currentTime = Instant.now();

    String cronExpression = "2 * 3 * * FRI";

    daprPreviewClient.scheduleJob(new ScheduleJobRequest("Job", currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    daprPreviewClient.deleteJob(new DeleteJobRequest("Job")).block();
  }
}
