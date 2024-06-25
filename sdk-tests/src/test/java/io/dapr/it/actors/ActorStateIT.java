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

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.services.springboot.StatefulActor;
import io.dapr.it.actors.services.springboot.StatefulActorService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ActorStateIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActorStateIT.class);

  /**
   * Parameters for this test.
   * Param #1: useGrpc.
   * @return Collection of parameter tuples.
   */
  public static Stream<Arguments> data() {
    return Stream.of(
                    Arguments.of(AppRun.AppProtocol.HTTP ),
                    Arguments.of(AppRun.AppProtocol.GRPC )
    );
  }

  @ParameterizedTest
  @MethodSource("data")
  public void writeReadState(AppRun.AppProtocol serviceAppProtocol) throws Exception {
    logger.debug("Starting actor runtime ...");
    // The call below will fail if service cannot start successfully.
    DaprRun runtime = startDaprApp(
      this.getClass().getSimpleName(),
      StatefulActorService.SUCCESS_MESSAGE,
      StatefulActorService.class,
      true,
      60000,
      serviceAppProtocol);

    String message = "This is a message to be saved and retrieved.";
    String name = "Jon Doe";
    byte[] bytes = new byte[] { 0x1 };
    ActorId actorId = new ActorId(
        String.format("%d-%b", System.currentTimeMillis(), serviceAppProtocol));
    String actorType = "StatefulActorTest";
    logger.debug("Building proxy ...");
    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(actorType, ActorProxy.class, newActorClient());
    ActorProxy proxy = proxyBuilder.build(actorId);

    // wating for actor to be activated
    Thread.sleep(2000);  
    
    // Validate conditional read works.
    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is not present yet ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertNull(result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeMessage ... ");
      proxy.invokeMethod("writeMessage", message).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is probably still cached ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertEquals(message, result);
    }, 5000);

    // writeData uses an object instead of String to test serialization.
    StatefulActor.MyData mydata = new StatefulActor.MyData();
    mydata.value = "My data value.";
    callWithRetry(() -> {
      logger.debug("Invoking writeData with object ... ");
      proxy.invokeMethod("writeData", mydata).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readData where data is probably still cached ... ");
      StatefulActor.MyData result = proxy.invokeMethod("readData", StatefulActor.MyData.class).block();
      assertEquals(mydata.value, result.value);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeName ... ");
      proxy.invokeMethod("writeName", name).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readName where data is probably still cached ... ");
      String result = proxy.invokeMethod("readName", String.class).block();
      assertEquals(name, result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeName with empty content... ");
      proxy.invokeMethod("writeName", "").block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readName where empty content is probably still cached ... ");
      String result = proxy.invokeMethod("readName", String.class).block();
      assertEquals("", result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeBytes ... ");
      proxy.invokeMethod("writeBytes", bytes).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readBytes where data is probably still cached ... ");
      byte[] result = proxy.invokeMethod("readBytes", byte[].class).block();
      assertArrayEquals(bytes, result);
    }, 5000);

    logger.debug("Waiting, so actor can be deactivated ...");
    Thread.sleep(10000);

    logger.debug("Stopping service ...");
    runtime.stop();

    logger.debug("Starting service ...");
    DaprRun run2 = startDaprApp(
        this.getClass().getSimpleName(),
        StatefulActorService.SUCCESS_MESSAGE,
        StatefulActorService.class,
        true,
        60000,
        serviceAppProtocol);

    // Need new proxy builder because the proxy builder holds the channel.
    proxyBuilder = new ActorProxyBuilder(actorType, ActorProxy.class, newActorClient());
    ActorProxy newProxy = proxyBuilder.build(actorId);

    // wating for actor to be activated
    Thread.sleep(2000);  

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is not cached ... ");
      String result = newProxy.invokeMethod("readMessage", String.class).block();
      assertEquals(message, result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readData where data is not cached ... ");
      StatefulActor.MyData result = newProxy.invokeMethod("readData", StatefulActor.MyData.class).block();
      assertEquals(mydata.value, result.value);
    }, 5000);
    logger.debug("Finished testing actor string state.");

    callWithRetry(() -> {
      logger.debug("Invoking readName where empty content is not cached ... ");
      String result = newProxy.invokeMethod("readName", String.class).block();
      assertEquals("", result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readBytes where content is not cached ... ");
      byte[] result = newProxy.invokeMethod("readBytes", byte[].class).block();
      assertArrayEquals(bytes, result);
    }, 5000);
  }
}
