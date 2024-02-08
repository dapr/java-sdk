package io.dapr.utils;

import io.dapr.config.Properties;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NetworkUtilsTest {
  private final int defaultGrpcPort = 4000;
  private final String defaultSidecarIP = "127.0.0.1";

  private ManagedChannel channel;
  @BeforeEach
  public void setUp() {
    System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(defaultGrpcPort));
    System.setProperty(Properties.SIDECAR_IP.getName(), defaultSidecarIP);
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "");
  }

  @AfterEach
  public void tearDown(){
    if (channel != null && !channel.isShutdown()) {
      channel.shutdownNow();
    }
  }


  @Test
  public void testBuildGrpcManagedChannel() {
    channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointNoPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "http://example.com");
    channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:80";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointWithPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "http://example.com:3000");
    channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:3000";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointNoPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "https://example.com");
    channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:443";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointWithPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "https://example.com:3000");
    channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:3000";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }
}
