/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dapr.it.testcontainers.support;

import com.google.protobuf.Empty;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.MethodInvokeServiceGrpc;
import io.dapr.it.MethodInvokeServiceProtos;
import io.dapr.it.testcontainers.methodinvoke.http.app.MethodInvokeService;
import io.dapr.testcontainers.DaprProtocol;
import io.dapr.v1.AppCallbackHealthCheckGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("testcontainers")
class DaprTestEnvironmentIT {
  private static final String LABEL = "io.dapr.it.environment";

  @Test
  void startsExactlyFourChildrenAndStopsOnlyItsOwnResources() throws Exception {
    DaprTestEnvironment environment = new DaprTestEnvironment("foundation-it", MethodInvokeService.class);

    try {
      environment.start();
      assertEquals(4, containers(environment.identity()));
      assertEquals(1, networks(environment.identity()));
      var network = DockerClientFactory.instance().client().listNetworksCmd()
          .withFilter("label", List.of(LABEL + "=" + environment.identity())).exec().get(0);
      var attached = DockerClientFactory.instance().client().inspectNetworkCmd()
          .withNetworkId(network.getId()).exec().getContainers();
      assertEquals(4, attached.size(), "unexpected containers attached to the private network");
      var mounts = DockerClientFactory.instance().client().inspectContainerCmd(environment.appContainerId())
          .exec().getMounts();
      var runtime = mounts.stream().filter(mount -> "/it-runtime".equals(mount.getDestination().getPath()))
          .findFirst().orElseThrow(() -> new AssertionError("application runtime is not mounted"));
      assertEquals(Boolean.FALSE, runtime.getRW(), "application runtime must be read-only");
      assertTrue(runtime.getSource().replace('\\', '/').endsWith("/sdk-tests/target/it-runtime"));

      try (DaprClient client = environment.clientBuilder().build()) {
        client.invokeMethod("foundation-it", "health", null, HttpExtension.GET).block();
      }
    } finally {
      environment.stop();
      environment.stop();
    }

    assertClean(environment.identity());
  }

  @Test
  void failedApplicationRollsBackAllOwnedResources() {
    String missingMainClass = "io.dapr.it.testcontainers.support.MissingApplication";
    DaprTestEnvironment environment = new DaprTestEnvironment("foundation-failure-it",
        missingMainClass, Duration.ofSeconds(5));
    assertThrows(RuntimeException.class, environment::start);
    assertTrue(environment.appOutput().contains(missingMainClass),
        "expected the failing application's logs, got: " + environment.appOutput());
    environment.stop();
    assertClean(environment.identity());
  }

  @Test
  void grpcCallbackAndRoutedInvocationAreReady() throws Exception {
    DaprTestEnvironment environment = new DaprTestEnvironment("foundation-grpc-it",
        io.dapr.it.testcontainers.methodinvoke.grpc.app.MethodInvokeService.class, DaprProtocol.GRPC);

    try {
      environment.start();
      assertEquals(4, containers(environment.identity()));
      assertEquals(1, networks(environment.identity()));
      var network = DockerClientFactory.instance().client().listNetworksCmd()
          .withFilter("label", List.of(LABEL + "=" + environment.identity())).exec().get(0);
      assertEquals(4, DockerClientFactory.instance().client().inspectNetworkCmd()
          .withNetworkId(network.getId()).exec().getContainers().size());
      assertTrue(environment.appOutput().contains("Artificial delay of 10 seconds"));
      assertThrows(IllegalStateException.class, environment::appEndpoint);
      ManagedChannel channel = ManagedChannelBuilder.forTarget(environment.appAddress()).usePlaintext().build();

      try {
        AppCallbackHealthCheckGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS)
            .healthCheck(Empty.getDefaultInstance());
      } finally {
        channel.shutdownNow();
        assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
      }

      try (DaprClient client = environment.clientBuilder().build()) {
        var stub = client.newGrpcStub("foundation-grpc-it", MethodInvokeServiceGrpc::newBlockingStub);
        assertTrue(stub.getMessages(MethodInvokeServiceProtos.GetMessagesRequest.getDefaultInstance())
            .getMessagesMap().isEmpty());
      }
    } finally {
      environment.stop();
      environment.stop();
    }

    assertClean(environment.identity());
  }

  private static long containers(String identity) {
    return DockerClientFactory.instance().client().listContainersCmd().withShowAll(true)
        .withLabelFilter(Map.of(LABEL, identity)).exec().size();
  }

  private static long networks(String identity) {
    return DockerClientFactory.instance().client().listNetworksCmd()
        .withFilter("label", List.of(LABEL + "=" + identity)).exec().size();
  }

  private static void assertClean(String identity) {
    assertTrue(containers(identity) == 0, "containers remain for " + identity);
    assertTrue(networks(identity) == 0, "network remains for " + identity);
  }
}
