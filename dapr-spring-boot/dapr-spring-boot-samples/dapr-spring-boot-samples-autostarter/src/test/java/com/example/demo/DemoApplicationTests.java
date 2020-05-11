/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package com.example.demo;

import io.dapr.client.DaprClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DemoApplicationTests {

  @Autowired
  private DaprClient client;

  @Test
  void contextLoads() {
    assertNotNull(this.client);
  }

}
