package io.dapr.it.testcontainers;

import java.util.Map;

import io.dapr.actors.runtime.ActorRuntime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.dapr.actors.client.ActorClient;
import io.dapr.config.Properties;

@Configuration
public class TestDaprActorsConfiguration {
  @Bean
  public ActorClient daprActorClient(
      @Value("${dapr.http.endpoint}") String daprHttpEndpoint,
      @Value("${dapr.grpc.endpoint}") String daprGrpcEndpoint
  ){
    Map<String, String> overrides = Map.of(
        "dapr.http.endpoint", daprHttpEndpoint,
        "dapr.grpc.endpoint", daprGrpcEndpoint
    );

    return new ActorClient(new Properties(overrides));
  }

  @Bean
  public ActorRuntime daprActorRuntime(
      @Value("${dapr.http.endpoint}") String daprHttpEndpoint,
      @Value("${dapr.grpc.endpoint}") String daprGrpcEndpoint
  ){
    Map<String, String> overrides = Map.of(
	  "dapr.http.endpoint", daprHttpEndpoint,
	  "dapr.grpc.endpoint", daprGrpcEndpoint
    );

    return ActorRuntime.getInstance(new Properties(overrides));
  }
}
