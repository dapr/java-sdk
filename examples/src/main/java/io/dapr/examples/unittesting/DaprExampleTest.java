/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.examples.unittesting;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. send a message to be saved as state:
 * java -jar target/dapr-java-sdk-examples-exec.jar \
 *     org.junit.platform.console.ConsoleLauncher --select-class=io.dapr.examples.unittesting.DaprExampleTest
 */
public class DaprExampleTest {

  @ActorType(name = "MyActor")
  public interface MyActor {
    String hello();
  }

  private static final class MyApp {

    private final DaprClient daprClient;

    private final Function<ActorId, MyActor> actorProxyFactory;

    /**
     * Example of constructor that can be used for production code.
     * @param client Dapr client.
     * @param actorClient Dapr Actor client.
     */
    public MyApp(DaprClient client, ActorClient actorClient) {
      this.daprClient = client;
      this.actorProxyFactory = (actorId) -> new ActorProxyBuilder<>(MyActor.class, actorClient).build(actorId);
    }

    /**
     * Example of constructor that can be used for test code.
     * @param client Dapr client.
     * @param actorProxyFactory Factory method to create actor proxy instances.
     */
    public MyApp(DaprClient client, Function<ActorId, MyActor> actorProxyFactory) {
      this.daprClient = client;
      this.actorProxyFactory = actorProxyFactory;
    }

    public String getState() {
      return daprClient.getState("appid", "statekey", String.class).block().getValue();
    }

    public String invokeActor() {
      MyActor proxy = actorProxyFactory.apply(new ActorId("myactorId"));
      return proxy.hello();
    }
  }

  @Test
  public void testGetState() {
    DaprClient daprClient = Mockito.mock(DaprClient.class);
    Mockito.when(daprClient.getState("appid", "statekey", String.class)).thenReturn(
        Mono.just(new State<>("statekey", "myvalue", "1")));

    MyApp app = new MyApp(daprClient, (ActorClient) null);

    String value = app.getState();

    assertEquals("myvalue", value);
  }

  @Test
  public void testInvokeActor() {
    MyActor actorMock = Mockito.mock(MyActor.class);
    Mockito.when(actorMock.hello()).thenReturn("hello world");

    MyApp app = new MyApp(null, actorId -> actorMock);

    String value = app.invokeActor();

    assertEquals("hello world", value);
  }
}
