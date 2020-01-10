package io.dapr.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.Empty;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprClientGrpcAdapterTest {

  private DaprGrpc.DaprFutureStub client;
  private DaprClientGrpcAdapter adater;

  @Before
  public void setup() {
    client = mock(DaprGrpc.DaprFutureStub.class);
    adater = new DaprClientGrpcAdapter(client);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unregisterActorTimerTest() {
    Mono<Void> result = adater.unregisterActorTimer("actorType",  "actorId", "timerName");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void registerActorTimerTest() {
    Mono<Void> result = adater.registerActorTimer("actorType",  "actorId", "timerName" , "DATA");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unregisterActorReminderTest() {
    Mono<Void> result = adater.unregisterActorReminder("actorType", "actorId", "reminderName");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void registerActorReminderTest() {
    Mono<Void> result = adater.registerActorReminder("actorType", "actorId", "reminderName", "DATA");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void saveActorStateTransactionallyTest() {
    Mono<Void> result = adater.saveActorStateTransactionally("actorType", "actorId", "DATA");
    result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getActorStateTest() {
    Mono<String> result = adater.getActorState("actorType", "actorId", "keyName");
    String state = result.block();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void invokeActorMethodTest() {
    Mono<String> result = adater.invokeActorMethod("actorType", "actorId", "methodName", "jsonPlayload");
    String monoResult = result.block();
  }

  @Test
  public void publishEventTest() {
    SettableFuture<Empty> settableFuture = SettableFuture.create();
    MockCallback<Empty> callback = new MockCallback<Empty>(Empty.newBuilder().build());
    addCallback(settableFuture, callback, directExecutor());
    settableFuture.set(Empty.newBuilder().build());
    when(client.publishEvent(any(DaprProtos.PublishEventEnvelope.class)))
        .thenReturn(settableFuture);
    Mono<Void> result = adater.publishEvent("topic", "object");
    result.block();
  }

  private final class MockCallback<T> implements FutureCallback<T> {
    @Nullable
    private T value = null;
    @Nullable
    private Throwable failure = null;
    private boolean wasCalled = false;

    public MockCallback(T expectedValue) {
      this.value = expectedValue;
    }

    public MockCallback(Throwable expectedFailure) {
      this.failure = expectedFailure;
    }

    @Override
    public synchronized void onSuccess(@NullableDecl T result) {
      assertFalse(wasCalled);
      wasCalled = true;
      assertEquals(value, result);
    }

    @Override
    public synchronized void onFailure(Throwable throwable) {
      assertFalse(wasCalled);
      wasCalled = true;
      assertEquals(failure, throwable);
    }
  }
}
