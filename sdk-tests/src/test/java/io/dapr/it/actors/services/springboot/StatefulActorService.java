/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.serializer.DefaultObjectSerializer;

import java.time.Duration;

public class StatefulActorService {

  public static final String SUCCESS_MESSAGE = "dapr initialized. Status: Running";

  /**
   * Starts the service.
   *
   * @param args Expects the port as only argument.
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {

    // If port string is not valid, it will throw an exception.
    long port = Long.parseLong(args[0]);
    ActorRuntime.getInstance().getConfig().setActorIdleTimeout(Duration.ofSeconds(5));
    ActorRuntime.getInstance().getConfig().setActorScanInterval(Duration.ofSeconds(2));
    ActorRuntime.getInstance().getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(10));
    ActorRuntime.getInstance().getConfig().setDrainBalancedActors(true);
    ActorRuntime.getInstance().registerActor(
      StatefulActorImpl.class, new DefaultObjectSerializer(), new DefaultObjectSerializer());

    DaprApplication.start(port);
  }
}
