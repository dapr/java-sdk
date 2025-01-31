package io.dapr.it.testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.config.Properties;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class TestDaprWorkflowsConfiguration {
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
    builder.registerActivity(FirstActivity.class);
    builder.registerActivity(SecondActivity.class);

    return builder;
  }
}
