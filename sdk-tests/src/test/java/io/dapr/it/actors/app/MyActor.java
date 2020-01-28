/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.app;

import java.util.ArrayList;
import java.util.List;

public interface MyActor {
  String say(String something);

  List<String> retrieveActiveActors();

  void startReminder(String name);

  void stopReminder(String name);

  void startTimer(String name);

  void stopTimer(String name);

  void clock(String message);

  ArrayList<String> getCallLog();

  String getIdentifier();
}