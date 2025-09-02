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
import io.dapr.testcontainers.DaprLogLevel;
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

import java.util.Collections;
import java.util.List;

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

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network network) {

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("workflow-patterns-app")
            .withComponent(new Component("kvstore", "state.in-memory", "v1", Collections.singletonMap("actorStateStore", String.valueOf(true))))
            .withAppPort(8080)
            .withNetwork(network)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal");
  }


  @Bean
  MicrocksContainersEnsemble microcksEnsemble(Network network) {
    return new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.2")
            .withAccessToHost(true)   // We need this to access our webapp while it runs
            .withMainArtifacts("third-parties/remote-http-service.yaml");
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
