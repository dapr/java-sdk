package io.dapr.examples.jobs;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.JobSchedule;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.config.Properties;
import io.dapr.config.Property;

import java.util.Map;

public class DemoJobsClient {

  /**
   * The main method of this app to register and fetch jobs.
   */
  public static void main(String[] args) throws Exception {
    Map<Property<?>, String> overrides = Map.of(
        Properties.HTTP_PORT, "3500",
        Properties.GRPC_PORT, "51439"
    );

    try (DaprPreviewClient client = new DaprClientBuilder().withPropertyOverrides(overrides).buildPreviewClient()) {

      // Schedule a job.
      ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest("dapr-job-1",
          JobSchedule.fromString("* * * * * *")).setData("Hello World!".getBytes());
      client.scheduleJob(scheduleJobRequest).block();

      // Get a job.
      GetJobResponse getJobResponse = client.getJob(new GetJobRequest("dapr-job-1")).block();
    }
  }
}
