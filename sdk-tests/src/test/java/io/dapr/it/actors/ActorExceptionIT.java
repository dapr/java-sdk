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
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.app.MyActor;
import io.dapr.it.actors.app.MyActorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprExceptionSubstring;


public class ActorExceptionIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActorExceptionIT.class);

  private static DaprRun run;

  @BeforeAll
  public static void start() throws Exception {
    // The call below will fail if service cannot start successfully.
    run = startDaprApp(
        ActorExceptionIT.class.getSimpleName(),
        MyActorService.SUCCESS_MESSAGE,
        MyActorService.class,
        true,
        60000);
  }

  @Test
  public void exceptionTest() throws Exception {
    ActorProxyBuilder<MyActor> proxyBuilder =
        new ActorProxyBuilder("MyActorTest", MyActor.class, deferClose(run.newActorClient()));
    MyActor proxy = proxyBuilder.build(new ActorId("1"));

    callWithRetry(() -> {
      assertThrowsDaprExceptionSubstring(
          "INTERNAL",
          "INTERNAL: error invoke actor method: error from actor service",
          () ->  proxy.throwException());
    }, 10000);
  }

  @Test
  public void exceptionDueToMetadataTest() throws Exception {
    // Setting this HTTP header via actor metadata will cause the Actor HTTP server to error.
    Map<String, String> metadata = Map.of("Content-Length", "9999");
    ActorProxyBuilder<MyActor> proxyBuilderMetadataOverride =
        new ActorProxyBuilder("MyActorTest", MyActor.class, deferClose(run.newActorClient(metadata)));

    MyActor proxyWithMetadata = proxyBuilderMetadataOverride.build(new ActorId("2"));
    callWithRetry(() -> {
      assertThrowsDaprExceptionSubstring(
          "INTERNAL",
          "ContentLength=9999 with Body length 13",
          () -> proxyWithMetadata.say("hello world"));
    }, 10000);
  }
}
