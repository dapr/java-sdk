/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.examples.jobs;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
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

    try (DaprClient client = new DaprClientBuilder().withPropertyOverrides(overrides).build()) {

      // Schedule a job.
      System.out.println("**** Scheduling a Job with name dapr-jobs-1 *****");
      ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest("dapr-job-1",
          JobSchedule.fromString("* * * * * *")).setData("Hello World!".getBytes());
      client.scheduleJob(scheduleJobRequest).block();

      System.out.println("**** Scheduling job dapr-jobs-1 completed *****");

      // Get a job.
      System.out.println("**** Retrieving a Job with name dapr-jobs-1 *****");
      GetJobResponse getJobResponse = client.getJob(new GetJobRequest("dapr-job-1")).block();
    }
  }
}
