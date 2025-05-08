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

package io.dapr.feign;


import feign.Body;
import feign.Feign;
import feign.Headers;
import feign.RequestLine;
import feign.Response;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DaprFeignClientBindingTest {

  @Mock
  DaprClient daprClient;

  @Test
  void DaprFeignClient_testMockBindingInvoke() {
    DaprFeignClientTestInterface repository =
        newBuilder().target(DaprFeignClientTestInterface.class, "http://binding.myBinding");

    assertEquals(200, repository.getWithContentType().status());
    assertEquals(0, repository.post().body().length());
  }

  public Feign.Builder newBuilder() {
    Mockito.when(daprClient.invokeBinding(Mockito.any(InvokeBindingRequest.class), Mockito.eq(TypeRef.BYTE_ARRAY)))
        .thenReturn(Mono.just(new byte[0]));

    return Feign.builder().client(new DaprInvokeFeignClient(daprClient));
  }

  public interface DaprFeignClientTestInterface {

    @RequestLine("GET /create")
    @Headers({"Accept: text/plain", "Content-Type: text/plain"})
    Response getWithContentType();

    @RequestLine("POST /get")
    @Body("test")
    Response post();
  }
}
