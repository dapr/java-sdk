/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.SubscribeConfigurationRequest;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.mockito.Mockito.times;

public class DaprPreviewClientProxyTest {

  @Test
  public void getconfigurationAPI() {
    DaprPreviewClient client = Mockito.mock(DaprPreviewClient.class);
    Mockito.when(client.getConfiguration("store", "key")).thenReturn(Mono.empty());
    Mockito.when(client.getConfigurations("store", "key")).thenReturn(Mono.empty());
    Mockito.when(client.getAllConfigurations("store")).thenReturn(Mono.empty());

    DaprPreviewClient proxy = new DaprPreviewClientProxy(client);

    proxy.getConfiguration("store", "key").block();
    proxy.getConfigurations("store", "key").block();
    proxy.getAllConfigurations("store").block();

    Mockito.verify(client, times(1)).getConfiguration("store", "key");
    Mockito.verify(client, times(1)).getConfigurations("store", "key");
    Mockito.verify(client, times(1)).getAllConfigurations("store");
  }

  @Test
  public void subscribeConfigurationAPI() {
    Map<String, String> metadata = new HashMap<>();
    List<String> keys = new ArrayList<>(Arrays.asList("key"));
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest("store", keys);
    DaprPreviewClient client = Mockito.mock(DaprPreviewClient.class);
    Mockito.when(client.subscribeToConfigurations("store", "key")).thenReturn(Flux.empty());

    DaprPreviewClient proxy = new DaprPreviewClientProxy(client);

    proxy.subscribeToConfigurations("store", "key");
    proxy.subscribeToConfigurations("store", keys, metadata);
    proxy.subscribeToConfigurations(req);

    Mockito.verify(client, times(1)).subscribeToConfigurations("store", "key");
    Mockito.verify(client, times(1)).subscribeToConfigurations("store", keys, metadata);
    Mockito.verify(client, times(1)).subscribeToConfigurations(req);
  }
}
