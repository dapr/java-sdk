/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.ActorType;

import java.util.List;

@ActorType(name = "DemoActorTest")
public interface DemoActor {
  String say(String something);

  List<String> retrieveActiveActors();

  void writeMessage(String something);

  String readMessage();
}
