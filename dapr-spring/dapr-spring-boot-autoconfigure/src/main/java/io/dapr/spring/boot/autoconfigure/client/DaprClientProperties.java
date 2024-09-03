package io.dapr.spring.boot.autoconfigure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dapr.client")
public record DaprClientProperties(String httpEndpoint, String grpcEndpoint, Integer httpPort, Integer grpcPort) {
}
