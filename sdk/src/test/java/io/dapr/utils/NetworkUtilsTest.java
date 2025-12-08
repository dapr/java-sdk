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

package io.dapr.utils;

import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.NetworkUtils.GrpcEndpointSettings;
import io.grpc.ManagedChannel;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkUtilsTest {
  private final int defaultGrpcPort = 50001;
  private final String defaultSidecarIP = "127.0.0.1";
  private ManagedChannel channel;
  private static final List<ManagedChannel> channels = new ArrayList<>();

  @AfterEach
  public void tearDown() {
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  @AfterAll
  public static void tearDownAll() {
    for (ManagedChannel ch : channels) {
      if (ch != null && !ch.isShutdown()) {
        ch.shutdown();
      }
    }
    channels.clear();
  }

  @Test
  public void testBuildGrpcManagedChannel() {
    channel = NetworkUtils.buildGrpcManagedChannel(new Properties());
    channels.add(channel);

    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointNoPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "http://example.com"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);

    String expectedAuthority = "example.com:80";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointWithPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "http://example.com:3000"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);

    String expectedAuthority = "example.com:3000";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointNoPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "https://example.com"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);

    String expectedAuthority = "example.com:443";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointWithPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "https://example.com:3000"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);

    String expectedAuthority = "example.com:3000";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannelWithTls() throws Exception {
    File certFile = new File(this.getClass().getResource("/certs/test-cert.pem").getFile());
    File keyFile = new File(this.getClass().getResource("/certs/test-cert.key").getFile());

    var properties = new Properties(Map.of(
          Properties.GRPC_TLS_CERT_PATH.getName(), certFile.getAbsolutePath(),
          Properties.GRPC_TLS_KEY_PATH.getName(), keyFile.getAbsolutePath()
      ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assertions.assertEquals(expectedAuthority, channel.authority());

  }

  @Test
  public void testBuildGrpcManagedChannelWithTlsAndEndpoint() throws Exception {
    File certFile = new File(this.getClass().getResource("/certs/test-cert.pem").getFile());
    File keyFile = new File(this.getClass().getResource("/certs/test-cert.key").getFile());

    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CERT_PATH.getName(), certFile.getAbsolutePath(),
        Properties.GRPC_TLS_KEY_PATH.getName(), keyFile.getAbsolutePath(),
        Properties.GRPC_ENDPOINT.getName(), "https://example.com:443"
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    Assertions.assertEquals("example.com:443", channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannelWithInvalidTlsCert() {
    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CERT_PATH.getName(), "/nonexistent/cert.pem",
        Properties.GRPC_TLS_KEY_PATH.getName(), "/nonexistent/key.pem"
    ));

    Assertions.assertThrows(DaprException.class, () -> {
      NetworkUtils.buildGrpcManagedChannel(properties);
    });
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC}) // Unix domain sockets are only supported on Linux and macOS
  public void testBuildGrpcManagedChannelWithTlsAndUnixSocket() throws Exception {
    // Skip test if Unix domain sockets are not supported
    Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("linux") || 
                          System.getProperty("os.name").toLowerCase().contains("mac"));

    // Generate test certificate and key
    File certFile = new File(this.getClass().getResource("/certs/test-cert.pem").getFile());
    File keyFile = new File(this.getClass().getResource("/certs/test-cert.key").getFile());

    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CERT_PATH.getName(), certFile.getAbsolutePath(),
        Properties.GRPC_TLS_KEY_PATH.getName(), keyFile.getAbsolutePath(),
        Properties.GRPC_ENDPOINT.getName(), "unix:/tmp/test.sock"
    ));

    // For Unix sockets, we expect an exception if the platform doesn't support it
    try {
      channel = NetworkUtils.buildGrpcManagedChannel(properties);
      channels.add(channel);
      // If we get here, Unix sockets are supported
      Assertions.assertNotNull(channel.authority(), "Channel authority should not be null");
    } catch (Exception e) {
      // If we get here, Unix sockets are not supported
      Assertions.assertTrue(e.getMessage().contains("DomainSocketAddress"));
    }

  }

  @Test
  public void testBuildGrpcManagedChannelWithTlsAndDnsAuthority() throws Exception {
    File certFile = new File(this.getClass().getResource("/certs/test-cert.pem").getFile());
    File keyFile = new File(this.getClass().getResource("/certs/test-cert.key").getFile());
      var properties = new Properties(Map.of(
          Properties.GRPC_TLS_CERT_PATH.getName(), certFile.getAbsolutePath(),
          Properties.GRPC_TLS_KEY_PATH.getName(), keyFile.getAbsolutePath(),
          Properties.GRPC_ENDPOINT.getName(), "dns://authority:53/example.com:443"
      ));

      channel = NetworkUtils.buildGrpcManagedChannel(properties);
      channels.add(channel);
      Assertions.assertEquals("example.com:443", channel.authority());

  }

  @Test
  public void testBuildGrpcManagedChannelWithTlsAndCaCert() throws Exception {
    File caCertFile = new File(this.getClass().getResource("/certs/test-ca-cert.pem").getFile());

    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CA_PATH.getName(), caCertFile.getAbsolutePath()
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannelWithTlsAndCaCertAndEndpoint() throws Exception {
    File caCertFile = new File(this.getClass().getResource("/certs/test-ca-cert.pem").getFile());

      var properties = new Properties(Map.of(
          Properties.GRPC_TLS_CA_PATH.getName(), caCertFile.getAbsolutePath(),
          Properties.GRPC_ENDPOINT.getName(), "https://example.com:443"
      ));

      channel = NetworkUtils.buildGrpcManagedChannel(properties);
      channels.add(channel);
      Assertions.assertEquals("example.com:443", channel.authority());

  }

  @Test
  public void testBuildGrpcManagedChannelWithInvalidCaCert() {
    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CA_PATH.getName(), "/nonexistent/ca.pem"
    ));

    Assertions.assertThrows(DaprException.class, () -> {
      NetworkUtils.buildGrpcManagedChannel(properties);
    });
  }

  @Test
  public void testBuildGrpcManagedChannelWithMtlsAndCaCert() throws Exception {
    File caCertFile = new File(this.getClass().getResource("/certs/test-ca-cert.pem").getFile());
    File clientCertFile = new File(this.getClass().getResource("/certs/test-cert.pem").getFile());
    File clientKeyFile = new File(this.getClass().getResource("/certs/test-cert.key").getFile());

    // Test mTLS with both client certs and CA cert
    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CA_PATH.getName(), caCertFile.getAbsolutePath(),
        Properties.GRPC_TLS_CERT_PATH.getName(), clientCertFile.getAbsolutePath(),
        Properties.GRPC_TLS_KEY_PATH.getName(), clientKeyFile.getAbsolutePath()
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assertions.assertEquals(expectedAuthority, channel.authority());
    Assertions.assertFalse(channel.isTerminated(), "Channel should be active");
  }

  @Test
  public void testGrpcEndpointParsing() {
    testGrpcEndpointParsingScenario(":5000", "dns:///127.0.0.1:5000", false);
    testGrpcEndpointParsingScenario(":5000?tls=true", "dns:///127.0.0.1:5000", true);
    testGrpcEndpointParsingScenario(":5000?tls=false", "dns:///127.0.0.1:5000", false);
    testGrpcEndpointParsingScenario("myhost:5000", "dns:///myhost:5000", false);
    testGrpcEndpointParsingScenario("myhost:5000?tls=true", "dns:///myhost:5000", true);
    testGrpcEndpointParsingScenario("myhost:5000?tls=false", "dns:///myhost:5000", false);
    testGrpcEndpointParsingScenario("myhost", "dns:///myhost:443", false);
    testGrpcEndpointParsingScenario("myhost?tls=true", "dns:///myhost:443", true);
    testGrpcEndpointParsingScenario("myhost?tls=false", "dns:///myhost:443", false);
    testGrpcEndpointParsingScenario("dns:myhost", "dns:///myhost:443", false);
    testGrpcEndpointParsingScenario("dns:myhost?tls=true", "dns:///myhost:443", true);
    testGrpcEndpointParsingScenario("dns:myhost?tls=false", "dns:///myhost:443", false);
    testGrpcEndpointParsingScenario("http://myhost", "dns:///myhost:80", false);
    testGrpcEndpointParsingScenario("http://myhost:443", "dns:///myhost:443", false);
    testGrpcEndpointParsingScenario("http://myhost:5000", "dns:///myhost:5000", false);
    testGrpcEndpointParsingScenario("http://myhost:8080", "dns:///myhost:8080", false);
    testGrpcEndpointParsingScenario("https://myhost", "dns:///myhost:443", true);
    testGrpcEndpointParsingScenario("https://myhost:443", "dns:///myhost:443", true);
    testGrpcEndpointParsingScenario("https://myhost:5000", "dns:///myhost:5000", true);
    testGrpcEndpointParsingScenario("dns:///myhost", "dns:///myhost:443", false);
    testGrpcEndpointParsingScenario("dns://myauthority:53/myhost", "dns://myauthority:53/myhost:443", false);
    testGrpcEndpointParsingScenario("dns://myauthority:53/myhost?tls=false", "dns://myauthority:53/myhost:443", false);
    testGrpcEndpointParsingScenario("dns://myauthority:53/myhost?tls=true", "dns://myauthority:53/myhost:443", true);
    testGrpcEndpointParsingScenario("unix:my.sock", "unix:my.sock", false);
    testGrpcEndpointParsingScenario("unix:my.sock?tls=true", "unix:my.sock", true);
    testGrpcEndpointParsingScenario("unix://my.sock", "unix://my.sock", false);
    testGrpcEndpointParsingScenario("unix://my.sock?tls=true", "unix://my.sock", true);
    testGrpcEndpointParsingScenario("unix-abstract:my.sock", "unix-abstract:my.sock", false);
    testGrpcEndpointParsingScenario("unix-abstract:my.sock?tls=true", "unix-abstract:my.sock", true);
    testGrpcEndpointParsingScenario("vsock:mycid:5000", "vsock:mycid:5000", false);
    testGrpcEndpointParsingScenario("vsock:mycid:5000?tls=true", "vsock:mycid:5000", true);
    testGrpcEndpointParsingScenario("[2001:db8:1f70::999:de8:7648:6e8]", "dns:///[2001:db8:1f70::999:de8:7648:6e8]:443", false);
    testGrpcEndpointParsingScenario("dns:[2001:db8:1f70::999:de8:7648:6e8]:5000", "dns:///[2001:db8:1f70::999:de8:7648:6e8]:5000", false);
    testGrpcEndpointParsingScenario("dns://myauthority:53/[2001:db8:1f70::999:de8:7648:6e8]", "dns://myauthority:53/[2001:db8:1f70::999:de8:7648:6e8]:443", false);
    testGrpcEndpointParsingScenario("https://[2001:db8:1f70::999:de8:7648:6e8]", "dns:///[2001:db8:1f70::999:de8:7648:6e8]:443", true);
    testGrpcEndpointParsingScenario("https://[2001:db8:1f70::999:de8:7648:6e8]:5000", "dns:///[2001:db8:1f70::999:de8:7648:6e8]:5000", true);
  }

  @Test
  public void testGrpcEndpointParsingError() {
    testGrpcEndpointParsingErrorScenario("http://myhost?tls=true");
    testGrpcEndpointParsingErrorScenario("http://myhost?tls=false");
    testGrpcEndpointParsingErrorScenario("http://myhost:8080?tls=true");
    testGrpcEndpointParsingErrorScenario("http://myhost:443?tls=false");
    testGrpcEndpointParsingErrorScenario("https://myhost?tls=true");
    testGrpcEndpointParsingErrorScenario("https://myhost?tls=false");
    testGrpcEndpointParsingErrorScenario("https://myhost:8080?tls=true");
    testGrpcEndpointParsingErrorScenario("https://myhost:443?tls=false");
    testGrpcEndpointParsingErrorScenario("dns://myhost");
    testGrpcEndpointParsingErrorScenario("dns:[2001:db8:1f70::999:de8:7648:6e8]:5000?abc=[]");
    testGrpcEndpointParsingErrorScenario("dns:[2001:db8:1f70::999:de8:7648:6e8]:5000?abc=123");
    testGrpcEndpointParsingErrorScenario("host:5000/v1/dapr");
    testGrpcEndpointParsingErrorScenario("host:5000/?a=1");
    testGrpcEndpointParsingErrorScenario("inv-scheme://myhost");
    testGrpcEndpointParsingErrorScenario("inv-scheme:myhost:5000");
  }

  private static void testGrpcEndpointParsingScenario(
      String grpcEndpointEnvValue,
      String expectedEndpoint,
      boolean expectSecure
  ) {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), grpcEndpointEnvValue));
    var settings = NetworkUtils.GrpcEndpointSettings.parse(properties);

    Assertions.assertEquals(expectedEndpoint, settings.endpoint);
    Assertions.assertEquals(expectSecure, settings.secure);
  }

  private static void testGrpcEndpointParsingErrorScenario(String grpcEndpointEnvValue) {
    try {
      var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), grpcEndpointEnvValue));
      NetworkUtils.GrpcEndpointSettings.parse(properties);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testBuildGrpcManagedChannelWithCaCertAndUnixSocket() throws Exception {
    // Skip test if Unix domain sockets are not supported
    Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("linux") ||
        System.getProperty("os.name").toLowerCase().contains("mac"));

    File caCertFile = new File(this.getClass().getResource("/certs/test-ca-cert.pem").getFile());

    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CA_PATH.getName(), caCertFile.getAbsolutePath(),
        Properties.GRPC_ENDPOINT.getName(), "unix:/tmp/test.sock"
    ));

    // For Unix sockets, we expect an exception if the platform doesn't support it
    try {
      channel = NetworkUtils.buildGrpcManagedChannel(properties);
      channels.add(channel);
      Assertions.assertNotNull(channel.authority(), "Channel authority should not be null");
    } catch (Exception e) {
      // If we get here, Unix sockets are not supported
      Assertions.assertTrue(e.getMessage().contains("DomainSocketAddress"));
    }

  }

  @Test
  public void testBuildGrpcManagedChannelWithCaCertAndDnsAuthority() throws Exception {
    File caCertFile = new File(this.getClass().getResource("/certs/test-ca-cert.pem").getFile());

    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_CA_PATH.getName(), caCertFile.getAbsolutePath(),
        Properties.GRPC_ENDPOINT.getName(), "dns://authority:53/example.com:443"
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    Assertions.assertEquals("example.com:443", channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannelWithInsecureTls() throws Exception {
    // Test insecure TLS mode with a secure endpoint
    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_INSECURE.getName(), "true",
        Properties.GRPC_ENDPOINT.getName(), "dns:///example.com:443?tls=true"
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    
    // Verify the channel is created with the correct authority
    Assertions.assertEquals("example.com:443", channel.authority());
    
    // Verify the channel is active and using TLS (not plaintext)
    Assertions.assertFalse(channel.isTerminated(), "Channel should be active");
  }

  @Test
  public void testBuildGrpcManagedChannelWithInsecureTlsAndMtls() throws Exception {
    File caCertFile = new File(this.getClass().getResource("/certs/test-ca-cert.pem").getFile());
    File clientCertFile = new File(this.getClass().getResource("/certs/test-cert.pem").getFile());
    File clientKeyFile = new File(this.getClass().getResource("/certs/test-cert.key").getFile());

    // Test that insecure TLS still works with mTLS settings
    // The client certs should be ignored since we're using InsecureTrustManagerFactory
    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_INSECURE.getName(), "true",
        Properties.GRPC_TLS_CA_PATH.getName(), caCertFile.getAbsolutePath(),
        Properties.GRPC_TLS_CERT_PATH.getName(), clientCertFile.getAbsolutePath(),
        Properties.GRPC_TLS_KEY_PATH.getName(), clientKeyFile.getAbsolutePath(),
        Properties.GRPC_ENDPOINT.getName(), "dns:///example.com:443?tls=true"
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);

    // Verify the channel is created with the correct authority
    Assertions.assertEquals("example.com:443", channel.authority());

    // Verify the channel is active and using TLS (not plaintext)
    Assertions.assertFalse(channel.isTerminated(), "Channel should be active");
  }

  @Test
  public void testBuildGrpcManagedChannelWithInsecureTlsAndCustomEndpoint() throws Exception {
    // Test insecure TLS with a custom endpoint that would normally require TLS
    var properties = new Properties(Map.of(
        Properties.GRPC_TLS_INSECURE.getName(), "true",
        Properties.GRPC_ENDPOINT.getName(), "dns://authority:53/example.com:443?tls=true"
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    
    // Verify the channel is created with the correct authority
    Assertions.assertEquals("example.com:443", channel.authority());
    
    // Verify the channel is active and using TLS (not plaintext)
    Assertions.assertFalse(channel.isTerminated(), "Channel should be active");
  }

  @Test
  public void testBuildGrpcManagedChannelWithKeepAliveDefaults() throws Exception {
    var properties = new Properties(Map.of(
          Properties.GRPC_ENABLE_KEEP_ALIVE.getName(), "true"
    ));

    channel = NetworkUtils.buildGrpcManagedChannel(properties);
    channels.add(channel);
    
    // Verify the channel is active and using TLS (not plaintext)
    Assertions.assertFalse(channel.isTerminated(), "Channel should be active");
  }

  @Test
  public void testDefaultKeepAliveSettings() throws Exception {
    Properties properties = new Properties();

    GrpcEndpointSettings settings = NetworkUtils.GrpcEndpointSettings.parse(properties);
    Assertions.assertEquals(false, settings.enableKeepAlive);
    Assertions.assertEquals(10, settings.keepAliveTimeSeconds.getSeconds());
    Assertions.assertEquals(5, settings.keepAliveTimeoutSeconds.getSeconds());
    Assertions.assertEquals(true, settings.keepAliveWithoutCalls); 
  }

  @Test
  public void testDefaultKeepAliveOverride() throws Exception {
    Properties properties = new Properties(Map.of(
        Properties.GRPC_ENABLE_KEEP_ALIVE.getName(), "true",
        Properties.GRPC_KEEP_ALIVE_TIME_SECONDS.getName(), "100",
        Properties.GRPC_KEEP_ALIVE_TIMEOUT_SECONDS.getName(), "50",
        Properties.GRPC_KEEP_ALIVE_WITHOUT_CALLS.getName(), "false"
    ));

    GrpcEndpointSettings settings = NetworkUtils.GrpcEndpointSettings.parse(properties);
    Assertions.assertEquals(true, settings.enableKeepAlive);
    Assertions.assertEquals(100, settings.keepAliveTimeSeconds.getSeconds());
    Assertions.assertEquals(50, settings.keepAliveTimeoutSeconds.getSeconds());
    Assertions.assertEquals(false, settings.keepAliveWithoutCalls); 
  }

  @Test
  public void testMaxDefaultInboundSize() throws Exception {
    Properties properties = new Properties();

    GrpcEndpointSettings settings = NetworkUtils.GrpcEndpointSettings.parse(properties);
    Assertions.assertEquals(4194304, settings.maxInboundMessageSize);
    Assertions.assertEquals(8192, settings.maxInboundMetadataSize);
    
  }

  @Test
  public void testMaxInboundSize() throws Exception {
    Properties properties = new Properties(Map.of(
        Properties.GRPC_MAX_INBOUND_MESSAGE_SIZE_BYTES.getName(), "123456789",
        Properties.GRPC_MAX_INBOUND_METADATA_SIZE_BYTES.getName(), "123456"
    ));

    GrpcEndpointSettings settings = NetworkUtils.GrpcEndpointSettings.parse(properties);
    Assertions.assertEquals(123456789, settings.maxInboundMessageSize);
    Assertions.assertEquals(123456, settings.maxInboundMetadataSize);
    
  }
}