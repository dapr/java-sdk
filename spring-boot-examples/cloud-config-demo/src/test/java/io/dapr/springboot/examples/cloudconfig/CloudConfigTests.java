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

package io.dapr.springboot.examples.cloudconfig;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.redis.testcontainers.RedisContainer;
import io.dapr.springboot.examples.cloudconfig.config.MultipleConfig;
import io.dapr.springboot.examples.cloudconfig.config.SingleConfig;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TestCloudConfigApplication.class, DaprTestContainersConfig.class,},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@Tag("testcontainers")
class CloudConfigTests {

  static {
    DaprTestContainersConfig.DAPR_CONTAINER.start();
  }

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8082;
    org.testcontainers.Testcontainers.exposeHostPorts(8082);
  }

  @Autowired
  MultipleConfig multipleConfig;

  @Autowired
  SingleConfig singleConfig;

  @Test
  void testCloudConfig() {
    assertEquals("testvalue", singleConfig.getSingleValueSecret());
    assertEquals("spring", multipleConfig.getMultipleSecretConfigV1());
    assertEquals("dapr", multipleConfig.getMultipleSecretConfigV2());
  }

  @Test
  void testController() {
    given().contentType(ContentType.JSON)
        .when()
        .get("/config")
        .then()
        .statusCode(200).body(is("testvalue"));
  }

  @Test
  void fillCoverage() {
    new SingleConfig().setSingleValueSecret("testvalue");
    new MultipleConfig().setMultipleSecretConfigV1("spring");
    new MultipleConfig().setMultipleSecretConfigV2("dapr");
  }

}
