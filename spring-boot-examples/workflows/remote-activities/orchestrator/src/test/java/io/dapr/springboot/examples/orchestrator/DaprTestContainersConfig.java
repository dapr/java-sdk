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

package io.dapr.springboot.examples.orchestrator;

import com.redis.testcontainers.RedisContainer;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

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

  private Map<String, String> getRedisProps(){
    Map<String, String> redisProps = new HashMap<>();
    redisProps.put("redisHost", "redis:6379");
    redisProps.put("redisPassword", "");
    redisProps.put("actorStateStore", String.valueOf(true));
    return redisProps;
  }

  @Bean("workerOneDapr")
  public DaprContainer workerOneDapr(Network daprNetwork, RedisContainer redisContainer, Environment env,
                                     @Qualifier("daprContainer") DaprContainer orchestratorDaprContainer) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("worker-one")
            .withNetworkAliases("worker-one-dapr")
            .withNetwork(daprNetwork)
            .withReusablePlacement(reuse)
            .withReusableScheduler(reuse)
            .withComponent(new Component("kvstore", "state.redis", "v1", getRedisProps()))
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppPort(8081)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(orchestratorDaprContainer)
            .dependsOn(redisContainer);
  }
  @Bean
  public GenericContainer<?> workerOneContainer(Network daprNetwork, @Qualifier("workerOneDapr") DaprContainer workerOneDapr){
    return new GenericContainer<>("openjdk:17-jdk-slim")
            .withCopyFileToContainer(MountableFile.forHostPath("../worker-one/target"), "/app")
            .withWorkingDirectory("/app")
            .withCommand("java",
                    "-Ddapr.grpc.endpoint=worker-one-dapr:50001",
                    "-Ddapr.http.endpoint=worker-one-dapr:3500",
                    "-jar",
                    "worker-one-1.17.0-SNAPSHOT.jar")
            .withNetwork(daprNetwork)
            .dependsOn(workerOneDapr)
            .waitingFor(Wait.forLogMessage(".*Started WorkerOneApplication.*", 1))
            .withLogConsumer(outputFrame -> System.out.println("WorkerOneApplication: " + outputFrame.getUtf8String()));
  }

  @Bean("workerTwoDapr")
  public DaprContainer workerTwoDapr(Network daprNetwork, RedisContainer redisContainer, Environment env,
                                     @Qualifier("daprContainer") DaprContainer orchestratorDaprContainer) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("worker-two")
            .withNetworkAliases("worker-two-dapr")
            .withNetwork(daprNetwork)
            .withReusablePlacement(reuse)
            .withReusableScheduler(reuse)
            .withComponent(new Component("kvstore", "state.redis", "v1", getRedisProps()))
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppPort(8082)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(orchestratorDaprContainer)
            .dependsOn(redisContainer);
  }
  @Bean
  public GenericContainer<?> workerTwoContainer(Network daprNetwork, @Qualifier("workerTwoDapr") DaprContainer workerTwoDapr){
    return new GenericContainer<>("openjdk:17-jdk-slim")
            .withCopyFileToContainer(MountableFile.forHostPath("../worker-two/target"), "/app")
            .withWorkingDirectory("/app")
            .withCommand("java",
                    "-Ddapr.grpc.endpoint=worker-two-dapr:50001",
                    "-Ddapr.http.endpoint=worker-two-dapr:3500",
                    "-jar",
                    "worker-two-1.17.0-SNAPSHOT.jar")
            .withNetwork(daprNetwork)
            .dependsOn(workerTwoDapr)
            .waitingFor(Wait.forLogMessage(".*Started WorkerTwoApplication.*", 1))
            .withLogConsumer(outputFrame -> System.out.println("WorkerTwoApplication: " + outputFrame.getUtf8String()));
  }


  @Bean
  public RedisContainer redisContainer(Network daprNetwork, Environment env){
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    return new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME)
            .withNetwork(daprNetwork)
            .withReuse(reuse)
            .withNetworkAliases("redis");
  }

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork, RedisContainer redisContainer, Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("orchestrator")
            .withNetwork(daprNetwork)
//            .withReusablePlacement(reuse)
//            .withReusableScheduler(reuse)
            .withComponent(new Component("kvstore", "state.redis", "v1", getRedisProps()))
//            .withDaprLogLevel(DaprLogLevel.DEBUG)
//            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppPort(8080)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(redisContainer);
  }



}
