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

package io.dapr.it.springboot4.testcontainers.jobs;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.DropFailurePolicy;
import io.dapr.client.domain.FailurePolicyType;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.it.springboot4.testcontainers.DaprClientConfiguration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.awaitility.Awaitility;
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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.dapr.it.springboot4.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
  private DaprClient daprClient;

  @BeforeEach
  public void setUp(){
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
  }

  @Test
  public void testJobScheduleCreationWithDueTime() {
    String jobName = randomJobName();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    Instant currentTime = newDueTime();
    daprClient.scheduleJob(new ScheduleJobRequest(jobName, currentTime).setOverwrite(true)).block();

    GetJobResponse getJobResponse = waitForJob(jobName);

    daprClient.deleteJob(new DeleteJobRequest(jobName)).block();

    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals(jobName, getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithSchedule() {
    String jobName = randomJobName();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    Instant currentTime = newDueTime();
    daprClient.scheduleJob(new ScheduleJobRequest(jobName, JobSchedule.hourly())
        .setDueTime(currentTime).setOverwrite(true)).block();

    GetJobResponse getJobResponse = waitForJob(jobName);

    daprClient.deleteJob(new DeleteJobRequest(jobName)).block();

    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals(JobSchedule.hourly().getExpression(), getJobResponse.getSchedule().getExpression());
    assertEquals(jobName, getJobResponse.getName());
  }

  @Test
  public void testJobScheduleCreationWithAllParameters() {
    String jobName = randomJobName();
    Instant currentTime = newDueTime();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest(jobName, currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setOverwrite(true)
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse = waitForJob(jobName);

    daprClient.deleteJob(new DeleteJobRequest(jobName)).block();

    assertEquals(iso8601Formatter.format(currentTime), getJobResponse.getDueTime().toString());
    assertEquals("2 * 3 * * FRI", getJobResponse.getSchedule().getExpression());
    assertEquals(jobName, getJobResponse.getName());
    assertEquals(Integer.valueOf(3), getJobResponse.getRepeats());
    assertEquals("Job data", new String(getJobResponse.getData()));
    assertEquals(iso8601Formatter.format(currentTime.plus(2, ChronoUnit.HOURS)),
            getJobResponse.getTtl().toString());
  }

  @Test
  public void testJobScheduleCreationWithDropFailurePolicy() {
    String jobName = randomJobName();
    Instant currentTime = newDueTime();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC);

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest(jobName, currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
            .setFailurePolicy(new DropFailurePolicy())
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse = waitForJob(jobName);

    daprClient.deleteJob(new DeleteJobRequest(jobName)).block();

    assertEquals(FailurePolicyType.DROP, getJobResponse.getFailurePolicy().getFailurePolicyType());
  }

  @Test
  public void testJobScheduleCreationWithConstantFailurePolicy() {
    String jobName = randomJobName();
    Instant currentTime = newDueTime();
    DateTimeFormatter iso8601Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC);

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest(jobName, currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setFailurePolicy(new ConstantFailurePolicy(3)
            .setDurationBetweenRetries(Duration.of(10, ChronoUnit.SECONDS)))
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    GetJobResponse getJobResponse = waitForJob(jobName);

    daprClient.deleteJob(new DeleteJobRequest(jobName)).block();

    ConstantFailurePolicy jobFailurePolicyConstant = (ConstantFailurePolicy) getJobResponse.getFailurePolicy();
    assertEquals(FailurePolicyType.CONSTANT, getJobResponse.getFailurePolicy().getFailurePolicyType());
    assertEquals(3, (int)jobFailurePolicyConstant.getMaxRetries());
    assertEquals(Duration.of(10, ChronoUnit.SECONDS).getNano(),
        jobFailurePolicyConstant.getDurationBetweenRetries().getNano());
  }

  @Test
  public void testDeleteJobRequest() {
    String jobName = randomJobName();
    Instant currentTime = newDueTime();

    String cronExpression = "2 * 3 * * FRI";

    daprClient.scheduleJob(new ScheduleJobRequest(jobName, currentTime)
        .setTtl(currentTime.plus(2, ChronoUnit.HOURS))
        .setData("Job data".getBytes())
        .setRepeat(3)
        .setOverwrite(true)
        .setSchedule(JobSchedule.fromString(cronExpression))).block();

    waitForJob(jobName);
    daprClient.deleteJob(new DeleteJobRequest(jobName)).block();
  }

  private String randomJobName() {
    return "job-" + UUID.randomUUID();
  }

  private Instant newDueTime() {
    return Instant.now().plus(1, ChronoUnit.MINUTES);
  }

  private GetJobResponse waitForJob(String jobName) {
    AtomicReference<GetJobResponse> responseRef = new AtomicReference<>();
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(() -> {
          GetJobResponse response = daprClient.getJob(new GetJobRequest(jobName)).block();
          assertNotNull(response);
          responseRef.set(response);
        });
    return responseRef.get();
  }
}
