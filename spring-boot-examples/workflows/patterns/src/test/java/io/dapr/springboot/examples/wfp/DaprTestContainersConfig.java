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

package io.dapr.springboot.examples.wfp;


import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.WorkflowDashboardContainer;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

/**
 * Test configuration for Dapr containers with debug logging enabled.
 * 
 * This configuration sets up Dapr with DEBUG log level and console output
 * for detailed logging during test execution.
 * 
 * ADDITIONAL DEBUGGING: For even more detailed logs, you can also:
 * 1. Run `docker ps` to find the Dapr container ID
 * 2. Run `docker logs --follow <container-id>` to stream real-time logs
 */
@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

  Map<String, String> postgreSQLDetails = new HashMap<>();

  {{
    postgreSQLDetails.put("host", "postgresql");
    postgreSQLDetails.put("user", "postgres");
    postgreSQLDetails.put("password", "postgres");
    postgreSQLDetails.put("database", "dapr");
    postgreSQLDetails.put("port", "5432");
    postgreSQLDetails.put("actorStateStore", String.valueOf(true));

  }}

  private Component stateStoreComponent = new Component("kvstore",
      "state.postgresql", "v2", postgreSQLDetails);

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network network, PostgreSQLContainer postgreSQLContainer) {

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("workflow-patterns-app")
            .withComponent(stateStoreComponent)
            .withAppPort(8080)
            .withNetwork(network)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal")
        .dependsOn(postgreSQLContainer);
  }

  @Bean
  public PostgreSQLContainer postgreSQLContainer(Network network) {
    return new PostgreSQLContainer(DockerImageName.parse("postgres"))
        .withNetworkAliases("postgresql")
        .withDatabaseName("dapr")
        .withUsername("postgres")
        .withPassword("postgres")
        .withNetwork(network);
  }

  @Bean
  MicrocksContainersEnsemble microcksEnsemble(Network network) {
    return new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.2")
            .withAccessToHost(true)   // We need this to access our webapp while it runs
            .withMainArtifacts("third-parties/remote-http-service.yaml");
  }

  @Bean
  public WorkflowDashboardContainer workflowDashboard(Network network) {
    return new WorkflowDashboardContainer(WorkflowDashboardContainer.getDefaultImageName())
            .withNetwork(network)
            .withStateStoreComponent(stateStoreComponent)
            .withExposedPorts(8080);
  }

  @Bean
  public DynamicPropertyRegistrar endpointsProperties(MicrocksContainersEnsemble ensemble) {
    // We need to replace the default endpoints with those provided by Microcks.
    return (properties) -> {
      properties.add("application.process-base-url", () -> ensemble.getMicrocksContainer()
              .getRestMockEndpoint("API Payload Processor", "1.0.0"));
    };
  }

  @Bean
  public Network getDaprNetwork(Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    if (reuse) {
      Network defaultDaprNetwork = new Network() {
        @Override
        public String getId() {
          return "dapr-network";
        }

        @Override
        public void close() {

        }

        @Override
        public Statement apply(Statement base, Description description) {
          return null;
        }
      };

      List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
              .withNameFilter("dapr-network").exec();
      if (networks.isEmpty()) {
        Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
        return defaultDaprNetwork;
      } else {
        return defaultDaprNetwork;
      }
    } else {
      return Network.newNetwork();
    }
  }

}
