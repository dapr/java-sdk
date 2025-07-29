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
import io.grpc.ManagedChannel;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;


public class NetworkUtilsTest {
  private final int defaultGrpcPort = 50001;
  private final String defaultSidecarIP = "127.0.0.1";
  private ManagedChannel channel;

  @AfterEach
  public void tearDown() {
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  @Test
  public void testBuildGrpcManagedChannel() {
    channel = NetworkUtils.buildGrpcManagedChannel(new Properties());

    String expectedAuthority = String.format("%s:%s", defaultSidecarIP, defaultGrpcPort);
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointNoPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "http://example.com"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);

    String expectedAuthority = "example.com:80";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpEndpointWithPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "http://example.com:3000"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);

    String expectedAuthority = "example.com:3000";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointNoPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "https://example.com"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);

    String expectedAuthority = "example.com:443";
    Assertions.assertEquals(expectedAuthority, channel.authority());
  }

  @Test
  public void testBuildGrpcManagedChannel_httpsEndpointWithPort() {
    var properties = new Properties(Map.of(Properties.GRPC_ENDPOINT.getName(), "https://example.com:3000"));
    channel = NetworkUtils.buildGrpcManagedChannel(properties);

    String expectedAuthority = "example.com:3000";
    Assertions.assertEquals(expectedAuthority, channel.authority());
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
  public void testMaxDefaultInboundSize() throws Exception {
    Properties properties = new Properties();

    NetworkUtils.GrpcEndpointSettings settings = NetworkUtils.GrpcEndpointSettings.parse(properties);
    Assertions.assertEquals(4194304, settings.maxInboundMessageSize);
    Assertions.assertEquals(8192, settings.maxInboundMetadataSize);

  }

  @Test
  public void testMaxInboundSize() throws Exception {
    Properties properties = new Properties(Map.of(
        Properties.GRPC_MAX_INBOUND_MESSAGE_SIZE_BYTES.getName(), "123456789",
        Properties.GRPC_MAX_INBOUND_METADATA_SIZE_BYTES.getName(), "123456"
    ));

    NetworkUtils.GrpcEndpointSettings settings = NetworkUtils.GrpcEndpointSettings.parse(properties);
    Assertions.assertEquals(123456789, settings.maxInboundMessageSize);
    Assertions.assertEquals(123456, settings.maxInboundMetadataSize);

  }
}