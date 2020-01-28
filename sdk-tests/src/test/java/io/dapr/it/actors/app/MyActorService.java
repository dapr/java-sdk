/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.app;

import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.serializer.DefaultObjectSerializer;

public class MyActorService {
  public static final String SUCCESS_MESSAGE = "actors: established connection to placement service at localhost";

  /**
   * Starts the service.
   * @param args Expects the port: -p PORT
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Hello from main() MyActorService");

    long port = Long.parseLong(args[0]);
    ActorRuntime.getInstance().registerActor(MyActorImpl.class, new DefaultObjectSerializer(), new DefaultObjectSerializer());

    TestApplication.start(port);
  }
}
