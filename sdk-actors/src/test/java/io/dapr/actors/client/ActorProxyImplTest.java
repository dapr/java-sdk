/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorMethod;
import io.dapr.actors.runtime.ActorInvocationContext;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorProxyImplTest {

  private ActorRuntime actorRuntime;

  @Before
  public void setup() {
    actorRuntime = mock(ActorRuntime.class);

    when(actorRuntime.getActorReentrancyId(anyString(), anyString())).thenReturn(Mono.just(Optional.empty()));
  }

  @Test()
  public void constructorActorProxyTest() {
    final ActorClient daprClient = mock(ActorClient.class);
    final DaprObjectSerializer serializer = mock(DaprObjectSerializer.class);
    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        serializer,
        daprClient,
        actorRuntime);
    Assert.assertEquals(actorProxy.getActorId().toString(), "100");
    Assert.assertEquals(actorProxy.getActorType(), "myActorType");
  }

  @Test()
  public void invokeActorMethodWithoutDataWithReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
            "{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
            .thenReturn(daprResponse);

    final ActorProxy actorProxy = new ActorProxyImpl(
            "myActorType",
            new ActorId("100"),
            new DefaultObjectSerializer(),
            daprClient,
            actorRuntime);

    Mono<MyData> result = actorProxy.invokeMethod("getData", MyData.class);
    MyData myData = result.block();
    assertNotNull(myData);
    Assert.assertEquals("valueA", myData.getPropertyA());
    Assert.assertEquals("valueB", myData.getPropertyB());// propertyB=null
  }

  @Test()
  public void invokeActorMethodWithoutDataWithReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    MyData myData = (MyData) actorProxy.invoke(actorProxy, Actor.class.getMethod("getData"), null);
    assertNotNull(myData);
    Assert.assertEquals("valueA", myData.getPropertyA());
    Assert.assertEquals("valueB", myData.getPropertyB());// propertyB=null
  }

  @Test()
  public void invokeActorMethodWithoutDataWithReturnMonoTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<MyData> res = (Mono<MyData>) actorProxy.invoke(actorProxy, Actor.class.getMethod("getDataMono"), null);
    assertNotNull(res);
    MyData myData = res.block();
    assertNotNull(myData);
    Assert.assertEquals("valueA", myData.getPropertyA());
    Assert.assertEquals("valueB", myData.getPropertyB());// propertyB=null
  }

  @Test()
  public void invokeActorMethodWithDataWithReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "\"OK\"".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes()), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    String res = (String) actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("echo", String.class),
        new Object[] { "hello world" } );

    Assert.assertEquals("OK", res);
  }

  @Test()
  public void invokeActorMethodWithDataWithReturnMonoTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.just(
        "\"OK\"".getBytes());

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes()), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<String> res = (Mono<String>) actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("echoMono", String.class),
        new Object[] { "hello world" } );

    assertNotNull(res);
    Assert.assertEquals("OK", res.block());
  }

  @Test()
  public void invokeActorMethodWithoutDataWithoutReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Object myData = actorProxy.invoke(actorProxy, Actor.class.getMethod("doSomething"), null);
    Assert.assertNull(myData);
  }

  @Test()
  public void invokeActorMethodWithoutDataWithoutReturnTypeMonoViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<Void> myData = (Mono<Void>)actorProxy.invoke(actorProxy, Actor.class.getMethod("doSomethingMono"), null);
    assertNotNull(myData);
    Assert.assertNull(myData.block());
  }

  @Test()
  public void invokeActorMethodWithDataWithoutReturnTypeMonoViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes()), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<Void> myData = (Mono<Void>)actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("doSomethingMonoWithArg", String.class),
        new Object[] { "hello world" });

    assertNotNull(myData);
    Assert.assertNull(myData.block());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void invokeActorMethodWithTooManyArgsViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<Void> myData = (Mono<Void>)actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("tooManyArgs", String.class, String.class),
        new Object[] { "hello", "world" });

    assertNotNull(myData);
    Assert.assertNull(myData.block());
  }

  @Test()
  public void invokeActorMethodWithDataWithoutReturnTypeViaReflection() throws NoSuchMethodException {
    final ActorClient daprClient = mock(ActorClient.class);
    Mono<byte[]> daprResponse = Mono.empty();

    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.eq("\"hello world\"".getBytes()), any()))
        .thenReturn(daprResponse);

    final ActorProxyImpl actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Object res = actorProxy.invoke(
        actorProxy,
        Actor.class.getMethod("process", String.class),
        new Object[] { "hello world" } );

    Assert.assertNull(res);
  }

  @Test()
  public void invokeActorMethodWithoutDataWithEmptyReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
        .thenReturn(Mono.just("".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<MyData> result = actorProxy.invokeMethod("getData", MyData.class);
    MyData myData = result.block();
    Assert.assertNull(myData);
  }

  @Test(expected = RuntimeException.class)
  public void invokeActorMethodWithIncorrectReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull()))
        .thenReturn(Mono.just("{test}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<MyData> result = actorProxy.invokeMethod("getData", MyData.class);

    result.doOnSuccess(x ->
        Assert.fail("Not exception was throw"))
        .doOnError(Throwable::printStackTrace
        ).block();
  }

  @Test()
  public void invokeActorMethodSavingDataWithReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull(), any()))
        .thenReturn(
          Mono.just("{\n\t\t\"propertyA\": \"valueA\",\n\t\t\"propertyB\": \"valueB\"\n\t}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    MyData myData = result.block();
    assertNotNull(myData);
    Assert.assertEquals("valueA", myData.getPropertyA());
    Assert.assertEquals("valueB", myData.getPropertyB());//propertyB=null

  }

  @Test(expected = DaprException.class)
  public void invokeActorMethodSavingDataWithIncorrectReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull(), any()))
        .thenReturn(Mono.just("{test}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    result.doOnSuccess(x ->
        Assert.fail("Not exception was throw"))
        .doOnError(Throwable::printStackTrace
        ).block();

  }

  @Test()
  public void invokeActorMethodSavingDataWithEmptyReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull(), any()))
        .thenReturn(Mono.just("".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    MyData myData = result.block();
    Assert.assertNull(myData);
  }


  @Test(expected = DaprException.class)
  public void invokeActorMethodSavingDataWithIncorrectInputType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull()))
        .thenReturn(Mono.just("{test}".getBytes()));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");
    saveData.setMyData(saveData);

    Mono<MyData> result = actorProxy.invokeMethod("getData", saveData, MyData.class);
    result.doOnSuccess(x ->
        Assert.fail("Not exception was throw"))
        .doOnError(Throwable::printStackTrace
        ).block();

  }

  @Test()
  public void invokeActorMethodWithDataWithVoidReturnType() {
    MyData saveData = new MyData();
    saveData.setPropertyA("valueA");
    saveData.setPropertyB("valueB");

    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNotNull(), any()))
        .thenReturn(Mono.empty());

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<Void> result = actorProxy.invokeMethod("getData", saveData);
    Void emptyResponse = result.block();
    Assert.assertNull(emptyResponse);
  }


  @Test(expected = DaprException.class)
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
        daprClient,
        actorRuntime);

    Mono<Void> result = actorProxy.invokeMethod("getData", saveData);
    Void emptyResponse = result.doOnError(Throwable::printStackTrace).block();
    Assert.assertNull(emptyResponse);
  }

  @Test()
  public void invokeActorMethodWithoutDataWithVoidReturnType() {
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), any()))
        .thenReturn(Mono.empty());

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    Mono<Void> result = actorProxy.invokeMethod("getData");
    Void emptyResponse = result.block();
    Assert.assertNull(emptyResponse);
  }

  @Test
  public void invokeActorMethodWithReentrancyIdSet() {
    final ArgumentCaptor<ActorInvocationContext> headerCaptor = ArgumentCaptor.forClass(ActorInvocationContext.class);
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), headerCaptor.capture()))
        .thenReturn(Mono.empty());

    final ActorRuntime reentrantRuntime = mock(ActorRuntime.class);
    when(reentrantRuntime.getActorReentrancyId(anyString(), anyString())).thenReturn(Mono.just(Optional.of("1")));

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        reentrantRuntime);

    actorProxy.invokeMethod("getData").block();
    final ActorInvocationContext context = headerCaptor.getValue();
    assertNotNull(context);
    assertNotNull(context.getHeaders());
    assertTrue(context.getHeaders().containsKey("Dapr-Reentrancy-Id"));
    assertEquals("1", context.getHeaders().get("Dapr-Reentrancy-Id"));
  }

  @Test
  public void invokeActorMethodWithoutReentrancyIdSet() {
    final ArgumentCaptor<ActorInvocationContext> headerCaptor = ArgumentCaptor.forClass(ActorInvocationContext.class);
    final ActorClient daprClient = mock(ActorClient.class);
    when(daprClient.invoke(anyString(), anyString(), anyString(), Mockito.isNull(), headerCaptor.capture()))
        .thenReturn(Mono.empty());

    final ActorProxy actorProxy = new ActorProxyImpl(
        "myActorType",
        new ActorId("100"),
        new DefaultObjectSerializer(),
        daprClient,
        actorRuntime);

    actorProxy.invokeMethod("getData").block();
    final ActorInvocationContext context = headerCaptor.getValue();
    assertNotNull(context);
    assertNotNull(context.getHeaders());
    assertTrue(context.getHeaders().isEmpty());
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
