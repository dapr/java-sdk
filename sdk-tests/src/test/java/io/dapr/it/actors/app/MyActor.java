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

package io.dapr.it.actors.app;

import io.dapr.actors.ActorMethod;

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

  void throwException();

  @ActorMethod(name = "DotNetMethodAsync")
  boolean dotNetMethod();
}