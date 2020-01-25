/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import java.util.List;

public interface DemoActor {
  String say(String something);

  List<String> retrieveActiveActors();
}
