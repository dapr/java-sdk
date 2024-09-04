package io.dapr.spring.boot.testcontainers.service.connection;

import io.dapr.spring.boot.autoconfigure.client.DaprConnectionDetails;
import io.dapr.testcontainers.DaprContainer;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

public class DaprContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<DaprContainer, DaprConnectionDetails> {

  DaprContainerConnectionDetailsFactory() {
  }

  protected DaprConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<DaprContainer> source) {
    return new DaprContainerConnectionDetails(source);
  }

  private static final class DaprContainerConnectionDetails
          extends ContainerConnectionDetailsFactory.ContainerConnectionDetails<DaprContainer>
          implements DaprConnectionDetails {
    private DaprContainerConnectionDetails(ContainerConnectionSource<DaprContainer> source) {
      super(source);
    }

    @Override
    public String httpEndpoint() {
      return getContainer().getHttpEndpoint();
    }

    @Override
    public String grpcEndpoint() {
      return getContainer().getGrpcEndpoint();
    }

    @Override
    public Integer httpPort() {
      return getContainer().getHttpPort();
    }

    @Override
    public Integer grpcPort() {
      return getContainer().getGrpcPort();
    }
  }
}
