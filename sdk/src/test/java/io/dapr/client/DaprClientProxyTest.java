/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.client.domain.HttpExtension;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.times;

public class DaprClientProxyTest {

  @Test
  public void stateAPI() {
    DaprClient client1 = Mockito.mock(DaprClient.class);
    DaprClient client2 = Mockito.mock(DaprClient.class);

    Mockito.when(client1.saveState("state", "key", "value")).thenReturn(Mono.empty());
    Mockito.when(client2.saveState("state", "key", "value")).thenReturn(Mono.empty());

    DaprClient proxy = new DaprClientProxy(client1, client2);
    proxy.saveState("state", "key", "value").block();

    Mockito.verify(client1, times(1)).saveState("state", "key", "value");
    Mockito.verify(client2, times(0)).saveState("state", "key", "value");
  }

  @Test
  public void methodInvocationAPI() {
    DaprClient client1 = Mockito.mock(DaprClient.class);
    DaprClient client2 = Mockito.mock(DaprClient.class);

    Mockito.when(client1.invokeMethod("appId", "methodName", "body", HttpExtension.POST))
        .thenReturn(Mono.empty());
    Mockito.when(client2.invokeMethod("appId", "methodName", "body", HttpExtension.POST))
        .thenReturn(Mono.empty());

    DaprClient proxy = new DaprClientProxy(client1, client2);
    proxy.invokeMethod("appId", "methodName", "body", HttpExtension.POST).block();

    Mockito.verify(client1, times(0))
        .invokeMethod("appId", "methodName", "body", HttpExtension.POST);
    Mockito.verify(client2, times(1))
        .invokeMethod("appId", "methodName", "body", HttpExtension.POST);
  }

  @Test
  public void closeAllClients() throws Exception {
    DaprClient client1 = Mockito.mock(DaprClient.class);
    DaprClient client2 = Mockito.mock(DaprClient.class);

    DaprClient proxy = new DaprClientProxy(client1, client2);
    proxy.close();

    Mockito.verify(client1, times(1)).close();
    Mockito.verify(client2, times(1)).close();
  }

  @Test
  public void closeSingleClient() throws Exception {
    DaprClient client1 = Mockito.mock(DaprClient.class);

    DaprClient proxy = new DaprClientProxy(client1);
    proxy.close();

    Mockito.verify(client1, times(1)).close();
  }

}
