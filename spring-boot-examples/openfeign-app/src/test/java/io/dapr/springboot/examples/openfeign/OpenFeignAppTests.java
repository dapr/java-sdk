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

package io.dapr.springboot.examples.openfeign;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.utils.TypeRef;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;


@SpringBootTest(classes = {TestConsumerApplication.class, DaprTestContainersConfig.class,
    DaprAutoConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OpenFeignAppTests {

  @MockitoBean
  private DaprClient daprClient;


  @BeforeAll
  public static void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(8083);
  }

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8083;
    Mockito.when(daprClient.invokeMethod(Mockito.any(InvokeMethodRequest.class), Mockito.eq(TypeRef.BYTE_ARRAY)))
        .thenReturn(Mono.just("[]".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void demoClientTest() {
    given().contentType(ContentType.JSON)
        .body("{ \"id\": \"abc-123\",\"item\": \"the mars volta LP\",\"amount\": 1}")
        .when()
        .post("/rpc/producer/orders")
        .then()
        .statusCode(200);

    given().contentType(ContentType.JSON)
        .when()
        .get("/rpc/producer/orders")
        .then()
        .statusCode(200).body("size()", is(0));

    given().contentType(ContentType.JSON)
        .when()
        .queryParam("item", "the mars volta LP")
        .get("/rpc/producer/orders/byItem/")
        .then()
        .statusCode(200).body("size()", is(0));
  }


}
