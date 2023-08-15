package io.dapr.utils;

import io.dapr.config.Properties;
import io.grpc.ManagedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NetworkUtilsTest {
  private final int defaultGrpcPort = 4000;
  private final String defaultSidecarIP = "127.0.0.1";
  @Before
  public void setUp() {
    System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(defaultGrpcPort));
    System.setProperty(Properties.SIDECAR_IP.getName(), defaultSidecarIP);
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "");
  }

  @Test
  public void testBuildGrpcManagedChannel() {
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assert.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointNoPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "http://example.com");
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:80";
    Assert.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointWithPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "http://example.com:3000");
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:3000";
    Assert.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointNoPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "https://example.com");
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:443";
    Assert.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointWithPort() {
    System.setProperty(Properties.GRPC_ENDPOINT.getName(), "https://example.com:3000");
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel();

    String expectedAuthority = "example.com:3000";
    Assert.assertEquals(expectedAuthority, channel.authority());
  }
}
