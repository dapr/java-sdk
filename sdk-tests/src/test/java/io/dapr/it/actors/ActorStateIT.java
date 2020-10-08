/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.services.springboot.StatefulActor;
import io.dapr.it.actors.services.springboot.StatefulActorService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.*;

public class ActorStateIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActorStateIT.class);

  @Test
  public void writeReadState() throws Exception {
    logger.debug("Starting actor runtime ...");
    // The call below will fail if service cannot start successfully.
    DaprRun runtime = startDaprApp(
      this.getClass().getSimpleName(),
      StatefulActorService.SUCCESS_MESSAGE,
      StatefulActorService.class,
      true,
      60000);

    String message = "This is a message to be saved and retrieved.";
    String name = "Jon Doe";
    byte[] bytes = new byte[] { 0x1 };
    ActorId actorId = new ActorId(Long.toString(System.currentTimeMillis()));
    String actorType = "StatefulActorTest";
    logger.debug("Building proxy ...");
    ActorProxyBuilder<ActorProxy> proxyBuilder = new ActorProxyBuilder(actorType, ActorProxy.class);
    ActorProxy proxy = proxyBuilder.build(actorId);

    // Validate conditional read works.
    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is not present yet ... ");
      String result = proxy.invokeActorMethod("readMessage", String.class).block();
      assertNull(result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeMessage ... ");
      proxy.invokeActorMethod("writeMessage", message).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is probably still cached ... ");
      String result = proxy.invokeActorMethod("readMessage", String.class).block();
      assertEquals(message, result);
    }, 5000);

    // writeData uses an object instead of String to test serialization.
    StatefulActor.MyData mydata = new StatefulActor.MyData();
    mydata.value = "My data value.";
    callWithRetry(() -> {
      logger.debug("Invoking writeData with object ... ");
      proxy.invokeActorMethod("writeData", mydata).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readData where data is probably still cached ... ");
      StatefulActor.MyData result = proxy.invokeActorMethod("readData", StatefulActor.MyData.class).block();
      assertEquals(mydata.value, result.value);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeName ... ");
      proxy.invokeActorMethod("writeName", name).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readName where data is probably still cached ... ");
      String result = proxy.invokeActorMethod("readName", String.class).block();
      assertEquals(name, result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeName with empty content... ");
      proxy.invokeActorMethod("writeName", "").block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readName where empty content is probably still cached ... ");
      String result = proxy.invokeActorMethod("readName", String.class).block();
      assertEquals("", result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeBytes ... ");
      proxy.invokeActorMethod("writeBytes", bytes).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readBytes where data is probably still cached ... ");
      byte[] result = proxy.invokeActorMethod("readBytes", byte[].class).block();
      assertArrayEquals(bytes, result);
    }, 5000);

    logger.debug("Waiting, so actor can be deactivated ...");
    Thread.sleep(10000);

    logger.debug("Stopping service ...");
    runtime.stop();

    logger.debug("Starting service ...");
    startDaprApp(
        this.getClass().getSimpleName(),
        StatefulActorService.SUCCESS_MESSAGE,
        StatefulActorService.class,
        true,
        60000);

    ActorProxy newProxy = proxyBuilder.build(actorId);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is not cached ... ");
      String result = newProxy.invokeActorMethod("readMessage", String.class).block();
      assertEquals(message, result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readData where data is not cached ... ");
      StatefulActor.MyData result = newProxy.invokeActorMethod("readData", StatefulActor.MyData.class).block();
      assertEquals(mydata.value, result.value);
    }, 5000);
    logger.debug("Finished testing actor string state.");

    callWithRetry(() -> {
      logger.debug("Invoking readName where empty content is not cached ... ");
      String result = newProxy.invokeActorMethod("readName", String.class).block();
      assertEquals("", result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readBytes where content is not cached ... ");
      byte[] result = newProxy.invokeActorMethod("readBytes", byte[].class).block();
      assertArrayEquals(bytes, result);
    }, 5000);
  }
}
