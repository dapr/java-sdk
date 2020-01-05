package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ActorStateSerializer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorProxyImplTest {

    @Test()
    public void constructorActorProxyTest() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        final ActorStateSerializer serializer = mock(ActorStateSerializer.class);
        final ActorProxyImpl actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                serializer,
                actorProxyAsyncClient);
        Assert.assertEquals(actorProxy.getActorId().toString(), "100");
        Assert.assertEquals(actorProxy.getActorType(), "myActorType");
    }

    @Test()
    public void invokeActorMethodWithoutDataWithReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNull()))
                .thenReturn(Mono.just("{\n" +
                        "\t\"data\": \"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"\n" +
                        "}"));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", MyData.class);
        MyData myData = result.block();
        Assert.assertNotNull(myData);
        Assert.assertEquals("valueA", myData.getPropertyA());
        Assert.assertEquals("valueB", myData.getPropertyB());
    }

    @Test()
    public void invokeActorMethodWithoutDataWithEmptyReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNull()))
                .thenReturn(Mono.just(""));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", MyData.class);
        MyData myData = result.block();
        Assert.assertNull(myData);
    }

    @Test(expected = RuntimeException.class)
    public void invokeActorMethodWithIncorrectReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNull()))
                .thenReturn(Mono.just("{test}"));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", MyData.class);

        result.doOnSuccess(x ->
                Assert.fail("Not exception was throw"))
                .doOnError(Throwable::printStackTrace
                ).block();


    }

    @Test()
    public void invokeActorMethodSavingDataWithReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNotNull()))
                .thenReturn(Mono.just("{\n" +
                        "\t\"data\": \"ewoJCSJwcm9wZXJ0eUEiOiAidmFsdWVBIiwKCQkicHJvcGVydHlCIjogInZhbHVlQiIKCX0=\"\n" +
                        "}"));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        MyData saveData = new MyData();
        saveData.setPropertyA("valueA");
        saveData.setPropertyB("valueB");

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", saveData, MyData.class);
        MyData myData = result.block();
        Assert.assertNotNull(myData);
        Assert.assertEquals("valueA", myData.getPropertyA());
        Assert.assertEquals("valueB", myData.getPropertyB());

    }

    @Test(expected = RuntimeException.class)
    public void invokeActorMethodSavingDataWithIncorrectReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNotNull()))
                .thenReturn(Mono.just("{test}"));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        MyData saveData = new MyData();
        saveData.setPropertyA("valueA");
        saveData.setPropertyB("valueB");

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", saveData, MyData.class);
        result.doOnSuccess(x ->
                Assert.fail("Not exception was throw"))
                .doOnError(Throwable::printStackTrace
                ).block();

    }

    @Test()
    public void invokeActorMethodSavingDataWithEmptyReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNotNull()))
                .thenReturn(Mono.just(""));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        MyData saveData = new MyData();
        saveData.setPropertyA("valueA");
        saveData.setPropertyB("valueB");

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", saveData, MyData.class);
        MyData myData = result.block();
        Assert.assertNull(myData);
    }


    @Test(expected = RuntimeException.class)
    public void invokeActorMethodSavingDataWithIncorrectInputType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNotNull()))
                .thenReturn(Mono.just("{test}"));

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        MyData saveData = new MyData();
        saveData.setPropertyA("valueA");
        saveData.setPropertyB("valueB");
        saveData.setMyData(saveData);

        Mono<MyData> result = actorProxy.invokeActorMethod("getData", saveData, MyData.class);
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

        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNotNull()))
                .thenReturn(Mono.empty());

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        Mono<Void> result = actorProxy.invokeActorMethod("getData", saveData);
        Void emptyResponse = result.block();
        Assert.assertNull(emptyResponse);
    }


    @Test(expected = RuntimeException.class)
    public void invokeActorMethodWithDataWithVoidIncorrectInputType() {
        MyData saveData = new MyData();
        saveData.setPropertyA("valueA");
        saveData.setPropertyB("valueB");
        saveData.setMyData(saveData);

        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNotNull()))
                .thenReturn(Mono.empty());

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        Mono<Void> result = actorProxy.invokeActorMethod("getData", saveData);
        Void emptyResponse = result.doOnError(Throwable::printStackTrace).block();
        Assert.assertNull(emptyResponse);
    }

    @Test()
    public void invokeActorMethodWithoutDataWithVoidReturnType() {
        final ActorProxyHttpAsyncClient actorProxyAsyncClient = mock(ActorProxyHttpAsyncClient.class);
        when(actorProxyAsyncClient.invokeActorMethod(anyString(), anyString(), anyString(), Mockito.isNull()))
                .thenReturn(Mono.empty());

        final ActorProxy actorProxy = new ActorProxyImpl(
                "myActorType",
                new ActorId("100"),
                new ActorStateSerializer(),
                actorProxyAsyncClient);

        Mono<Void> result = actorProxy.invokeActorMethod("getData");
        Void emptyResponse = result.block();
        Assert.assertNull(emptyResponse);
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
