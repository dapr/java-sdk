package io.dapr.spring.boot.autoconfigure.client;


import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface DaprConnectionDetails extends ConnectionDetails {
  String httpEndpoint();
  String grpcEndpoint();
  Integer httpPort();
  Integer grcpPort();
}
