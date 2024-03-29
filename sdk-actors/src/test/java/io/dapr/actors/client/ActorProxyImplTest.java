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

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorMethod;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorProxyImplTest {

  @Test()
  public void constructorActorProxyTest() {
    final ActorClient daprClient = mock(ActorClient.class);
    final DaprObjectSerializer serializer = mock(DaprObjectSerializer.class);
    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        serializer,
        daprClient);
    Assertions.assertEquals(actorProxy.getActorId().toString(), "100");
    Assertions.assertEquals(actorProxy.getActorType(), "myActorType");
  }

  @Test()
  public void invokeActorMethodWithoutDataWithReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
            "{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
            .thenReturn(daprResponse);

    final ActorProxy actorProxy = new ActorProxyImpl(
            "myActorType",
            new ActorId("100"),
            new DefaultObjectSerializer(),
            daprClient);

    Mono<MyData> result = actorProxy.invokeMethod("getData", MyData.class);
    MyData myData = result.block();
    Assertions.assertNotNull(myData);
    Assertions.assertEquals("valueA", myData.getPropertyA());
    Assertions.assertEquals("valueB", myData.getPropertyB());// propertyB=null
  }

  @Test()
  public void invokeActorMethodWithoutDataWithReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    MyData myData = (MyData) actorProxy.invoke(actorProxy, Actor.class.getMethod("getData"), null);
    Assertions.assertNotNull(myData);
    Assertions.assertEquals("valueA", myData.getPropertyA());
    Assertions.assertEquals("valueB", myData.getPropertyB());// propertyB=null
  }

  @Test()
  public void invokeActorMethodWithoutDataWithReturnMonoTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<MyData> res = (Mono<MyData>) actorProxy.invoke(actorProxy, Actor.class.getMethod("getDataMono"), null);
    Assertions.assertNotNull(res);
    MyData myData = res.block();
    Assertions.assertNotNull(myData);
    Assertions.assertEquals("valueA", myData.getPropertyA());
    Assertions.assertEquals("valueB", myData.getPropertyB());// propertyB=null
  }

  @Test()
  public void invokeActorMethodWithDataWithReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "\"OK\"".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes())))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    String res = (String) actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("echo", String.class),
        new Object[] { "hello world" } );

    Assertions.assertEquals("OK", res);
  }

  @Test()
  public void invokeActorMethodWithDataWithReturnMonoTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "\"OK\"".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes())))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<String> res = (Mono<String>) actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("echoMono", String.class),
        new Object[] { "hello world" } );

    Assertions.assertNotNull(res);
    Assertions.assertEquals("OK", res.block());
  }

  @Test()
  public void invokeActorMethodWithoutDataWithoutReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Object myData = actorProxy.invoke(actorProxy, Actor.class.getMethod("doSomething"), null);
    Assertions.assertNull(myData);
  }

  @Test()
  public void invokeActorMethodWithoutDataWithoutReturnTypeMonoViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<Void> myData = (Mono<Void>)actorProxy.invoke(actorProxy, Actor.class.getMethod("doSomethingMono"), null);
    Assertions.assertNotNull(myData);
    Assertions.assertNull(myData.block());
  }

  @Test()
  public void invokeActorMethodWithDataWithoutReturnTypeMonoViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes())))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<Void> myData = (Mono<Void>)actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("doSomethingMonoWithArg", String.class),
        new Object[] { "hello world" });

    Assertions.assertNotNull(myData);
    Assertions.assertNull(myData.block());
  }

  @Test
  public void invokeActorMethodWithTooManyArgsViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    assertThrows(UnsupportedOperationException.class, () -> actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("tooManyArgs", String.class, String.class),
        new Object[] { "hello", "world" }));
  }

  @Test()
  public void invokeActorMethodWithDataWithoutReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes())))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Object res = actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("process", String.class),
        new Object[] { "hello world" } );

    Assertions.assertNull(res);
  }

  @Test()
  public void invokeActorMethodWithoutDataWithEmptyReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(Mono.just("".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<MyData> result = actorProxy.invokeMethod("getData", MyData.class);
    MyData myData = result.block();
    Assertions.assertNull(myData);
  }

  @Test
  public void invokeActorMethodWithIncorrectReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(Mono.just("{test}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<MyData> result = actorProxy.invokeMethod("getData", MyData.class);

    assertThrows(DaprException.class, () ->result.block());
  }

  @Test()
  public void invokeActorMethodSavingDataWithReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(
          Mono.just("{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    MyData myData = result.block();
    Assertions.assertNotNull(myData);
    Assertions.assertEquals("valueA", myData.getPropertyA());
    Assertions.assertEquals("valueB", myData.getPropertyB());//propertyB=null

  }

  @Test
  public void invokeActorMethodSavingDataWithIncorrectReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(Mono.just("{test}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    assertThrows(DaprException.class, () ->result.block());

  }

  @Test()
  public void invokeActorMethodSavingDataWithEmptyReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(Mono.just("".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    MyData myData = result.block();
    Assertions.assertNull(myData);
  }


  @Test
  public void invokeActorMethodSavingDataWithIncorrectInputType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(Mono.just("{test}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");
    saveData.setMyData(saveData);

    assertThrows(DaprException.class, () -> actorProxy.invokeMethod("getData", saveData, MyData.class));
  }

  @Test()
  public void invokeActorMethodWithDataWithVoidReturnType() {
    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(Mono.empty());

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<Void> result = actorProxy.invokeMethod("getData", saveData);
    Void emptyResponse = result.block();
    Assertions.assertNull(emptyResponse);
  }


  @Test
  public void invokeActorMethodWithDataWithVoidIncorrectInputType() {
    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");
    saveData.setMyData(saveData);

    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(Mono.empty());

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    assertThrows(DaprException.class, () -> actorProxy.invokeMethod("getData", saveData));
  }

  @Test()
  public void invokeActorMethodWithoutDataWithVoidReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(Mono.empty());

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient);

    Mono<Void> result = actorProxy.invokeMethod("getData");
    Void emptyResponse = result.block();
    Assertions.assertNull(emptyResponse);
  }

  interface Actor {
    MyData getData();

    String echo(String message);

    @ActorMethod(returns = MyData.class)
    Mono<MyData> getDataMono();

    @ActorMethod(returns = String.class)
    Mono<String> echoMono(String message);

    void doSomething();

    Mono<Void> doSomethingMono();

    void process(String something);

    Mono<Void> doSomethingMonoWithArg(String something);

    void tooManyArgs(String something, String something2);
  }

  static class MyData {

    /// Gets or sets the value for PropertyA.
    private String propertyA;

    /// Gets or sets the value for PropertyB.
    private String propertyB;

    private MyData myData;


    public String getPropertyB() {
      return propertyB;
    }

    public void setPropertyB(String propertyB) {
      this.propertyB = propertyB;
    }

    public String getPropertyA() {
      return propertyA;
    }

    public void setPropertyA(String propertyA) {
      this.propertyA = propertyA;
    }

    @Override
    public String toString() {
      return "MyData{" +
          "propertyA='" + propertyA + '\'' +
          ", propertyB='" + propertyB + '\'' +
          '}';
    }

    public MyData getMyData() {
      return myData;
    }

    public void setMyData(MyData myData) {
      this.myData = myData;
    }
  }

}
