/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class ActorClientStubTest {

  @ActorType(name = "MyActor")
  public interface MyActor {

    void setName(String name);

    String getName();
  }

  /**
   * Example using ActorClientStub to have a mock implementation of an Actor invocation.
   */
  @Test
  public void testMocking() {
    // Changing this test can mean a breaking change since apps might mock based on some internal behavior of the SDK.
    ActorClientStub mock = Mockito.mock(ActorClientStub.class);
    Mockito.when(
        mock.invoke("MyActor", "100", "getName", null))
        .thenReturn(Mono.just("\"mock result 100\"".getBytes(StandardCharsets.UTF_8)));
    Mockito.when(
            mock.invoke("MyActor", "100", "setName", "\"My Name\"".getBytes(StandardCharsets.UTF_8)))
        .thenReturn(Mono.just("".getBytes(StandardCharsets.UTF_8)));

    ActorProxyBuilder<MyActor> builder = new ActorProxyBuilder(MyActor.class, mock);
    MyActor actor = builder.build(new ActorId("100"));

    String result = actor.getName();

    assertEquals("mock result 100", result);

    // Should not throw any exception and just succeed.
    actor.setName("My Name");
  }
}
