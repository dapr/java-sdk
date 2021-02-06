/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.app;

import io.dapr.actors.runtime.ActorRuntime;

public class MyActorService {
  public static final String SUCCESS_MESSAGE = "dapr initialized. Status: Running";

  /**
   * Starts the service.
   * @param args Expects the port: -p PORT
   * @throws Exception If cannot start service.
   */
  public static void main(String[] args) throws Exception {
    System.out.println("Hello from main() MyActorService");

    long port = Long.parseLong(args[0]);
    ActorRuntime.getInstance().registerActor(MyActorImpl.class);

    TestApplication.start(port);
  }
}
