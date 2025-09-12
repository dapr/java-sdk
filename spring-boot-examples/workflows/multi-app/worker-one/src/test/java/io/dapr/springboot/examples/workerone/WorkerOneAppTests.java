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

package io.dapr.springboot.examples.workerone;

import io.dapr.client.DaprClient;
import io.dapr.testcontainers.DaprContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TestWorkerOneApplication.class, DaprTestContainersConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WorkerOneAppTests {

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8081;
    org.testcontainers.Testcontainers.exposeHostPorts(8081);

  }

  @Test
  void testWorkerOne() {
    //Test the logic of the worker one
  }

}
