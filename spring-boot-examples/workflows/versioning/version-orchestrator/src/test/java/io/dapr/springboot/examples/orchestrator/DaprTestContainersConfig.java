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
limitations under the License.
*/

package io.dapr.springboot.examples.orchestrator;

import com.redis.testcontainers.RedisContainer;
import io.dapr.testcontainers.*;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.*;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

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

  private Map<String, String> getRedisProps() {
    Map<String, String> redisProps = new HashMap<>();
    redisProps.put("redisHost", "redis:6379");
    redisProps.put("redisPassword", "");
    redisProps.put("actorStateStore", String.valueOf(true));
    return redisProps;
  }

  @Bean
  public DaprPlacementContainer placementContainer(Network daprNetwork, Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    return new DaprPlacementContainer(DockerImageName.parse(DAPR_PLACEMENT_IMAGE_TAG))
        .withNetwork(daprNetwork)
        .withReuse(reuse)
        .withNetworkAliases("placement");
  }

  @Bean
  public DaprSchedulerContainer schedulerContainer(Network daprNetwork, Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    return new DaprSchedulerContainer(DockerImageName.parse(DAPR_SCHEDULER_IMAGE_TAG))
        .withNetwork(daprNetwork)
        .withReuse(reuse)
        .withNetworkAliases("scheduler");
  }

  @Bean
  public RedisContainer redisContainer(Network daprNetwork, Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    return new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME)
        .withNetwork(daprNetwork)
        .withReuse(reuse)
        .withNetworkAliases("redis");
  }

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork, RedisContainer redisContainer,
                                     Environment env,
                                     DaprPlacementContainer daprPlacementContainer,
                                     DaprSchedulerContainer daprSchedulerContainer) {

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("version-orchestrator")
        .withNetwork(daprNetwork)
        .withPlacementContainer(daprPlacementContainer)
        .withSchedulerContainer(daprSchedulerContainer)
        .withComponent(new Component("kvstore", "state.redis", "v1", getRedisProps()))
        .withAppPort(8080)
        .withAppHealthCheckPath("/actuator/health")
        .withAppChannelAddress("host.testcontainers.internal")
        .dependsOn(daprPlacementContainer)
        .dependsOn(daprSchedulerContainer)
        .dependsOn(redisContainer);
  }

}
