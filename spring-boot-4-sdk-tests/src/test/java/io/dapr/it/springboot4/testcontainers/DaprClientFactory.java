package io.dapr.it.springboot4.testcontainers;

import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.testcontainers.DaprContainer;

public interface DaprClientFactory {

  static DaprClientBuilder createDaprClientBuilder(DaprContainer daprContainer) {
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, "http://localhost:" + daprContainer.getHttpPort())
        .withPropertyOverride(Properties.GRPC_ENDPOINT, "http://localhost:" + daprContainer.getGrpcPort());
  }
}
