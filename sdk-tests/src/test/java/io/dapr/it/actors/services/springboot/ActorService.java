/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.serializer.DefaultObjectSerializer;

public class ActorService {

  /**
   * Starts the service.
   *
   * @param args Expects the port as only argument.
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {

    // If port string is not valid, it will throw an exception.
    long port = Long.parseLong(args[0]);
    ActorRuntime.getInstance().registerActor(
      DemoActorImpl.class, new DefaultObjectSerializer(), new DefaultObjectSerializer());

    DaprApplication.start(port);
  }
}
