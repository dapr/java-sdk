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


package io.dapr.spring.openfeign;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.spring.openfeign.client.DaprInvokeBindingClient;
import io.dapr.spring.openfeign.client.DaprInvokeMethodClient;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DaprOpenFeignClientTests {

  @MockBean
  DaprClient daprClient;

  @Autowired
  DaprInvokeBindingClient invokeBindingClient;

  @Autowired
  DaprInvokeMethodClient invokeMethodClient;

  @Test
  public void daprInvokeMethodTest() {
    Mockito.when(daprClient.invokeMethod(Mockito.any(InvokeMethodRequest.class), Mockito.eq(TypeRef.BYTE_ARRAY)))
        .thenReturn(Mono.just("Hello World!".getBytes(StandardCharsets.UTF_8)));

    assertEquals("Hello World!", invokeMethodClient.getQuery());
  }

  @Test
  public void daprInvokeBindingTest() {
    Mockito.when(daprClient.invokeBinding(Mockito.any(InvokeBindingRequest.class), Mockito.eq(TypeRef.BYTE_ARRAY)))
        .thenReturn(Mono.just("Hello World!".getBytes(StandardCharsets.UTF_8)));

    assertEquals("Hello World!", invokeBindingClient.getQuery());
  }

}
