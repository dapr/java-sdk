package io.dapr.actors.it.services.springboot;

import java.util.List;

public interface DemoActor {
  String say(String something);

  List<String> retrieveActiveActors();
}
