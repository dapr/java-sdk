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

package io.dapr.it.springboot4.testcontainers.workflows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.config.Properties;
import io.dapr.workflows.WorkflowActivityContext;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import io.dapr.workflows.WorkflowActivity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class TestWorkflowsConfiguration {
  @Bean
  public ObjectMapper mapper() {
    return new ObjectMapper();
  }

  @Bean
  public DaprWorkflowClient daprWorkflowClient(
      @Value("${dapr.http.endpoint}") String daprHttpEndpoint,
      @Value("${dapr.grpc.endpoint}") String daprGrpcEndpoint
  ){
    Map<String, String> overrides = Map.of(
        "dapr.http.endpoint", daprHttpEndpoint,
        "dapr.grpc.endpoint", daprGrpcEndpoint
    );

    return new DaprWorkflowClient(new Properties(overrides));
  }

  @Bean
  public WorkflowRuntimeBuilder workflowRuntimeBuilder(
      @Value("${dapr.http.endpoint}") String daprHttpEndpoint,
      @Value("${dapr.grpc.endpoint}") String daprGrpcEndpoint
  ){
      Map<String, String> overrides = Map.of(
              "dapr.http.endpoint", daprHttpEndpoint,
              "dapr.grpc.endpoint", daprGrpcEndpoint
      );

      WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder(new Properties(overrides));

      builder.registerWorkflow(TestWorkflow.class);
      builder.registerWorkflow(TestExecutionKeysWorkflow.class);
      builder.registerWorkflow(TestNamedActivitiesWorkflow.class);

      builder.registerActivity(FirstActivity.class);
      builder.registerActivity(SecondActivity.class);
      builder.registerActivity(TaskExecutionIdActivity.class);

      builder.registerActivity("a", FirstActivity.class);
      builder.registerActivity("b", FirstActivity.class);
      builder.registerActivity("c", new SecondActivity());
      builder.registerActivity("d", new WorkflowActivity() {
          @Override
          public Object run(WorkflowActivityContext ctx) {
              TestWorkflowPayload workflowPayload = ctx.getInput(TestWorkflowPayload.class);
              workflowPayload.getPayloads().add("Anonymous Activity");
              return workflowPayload;
          }
      });
      builder.registerActivity("e", new WorkflowActivity() {
          @Override
          public Object run(WorkflowActivityContext ctx) {
              TestWorkflowPayload workflowPayload = ctx.getInput(TestWorkflowPayload.class);
              workflowPayload.getPayloads().add("Anonymous Activity 2");
              return workflowPayload;
          }
      });

      return builder;
  }
}
