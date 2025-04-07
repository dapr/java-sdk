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
