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

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class DemoActorImpl extends AbstractActor implements DemoActor {

  public static final List<String> ACTIVE_ACTOR = new ArrayList<>();

  public DemoActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);
  }

  /**
   * Format to output date and time.
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  @Override
  public String say(String something) {
    Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

    // Handles the request by printing message.
    System.out.println("Server say method for actor " +
      super.getId() + ": " +
      (something == null ? "" : something + " @ " + utcNowAsString));

    // Now respond with current timestamp.
    return utcNowAsString;
  }

  @Override
  public List<String> retrieveActiveActors() {
    return Collections.unmodifiableList(ACTIVE_ACTOR);
  }

  @Override
  public void writeMessage(String something) {
    super.getActorStateManager().set("message", something).block();
  }

  @Override
  public String readMessage() {
    if (super.getActorStateManager().contains("message").block()) {
      return super.getActorStateManager().get("message", String.class).block();
    }

    return null;
  }

  @Override
  protected Mono<Void> onActivate() {
    return Mono.fromRunnable(() -> ACTIVE_ACTOR.add(super.getId().toString())).then(super.onActivate());
  }

  @Override
  protected Mono<Void> onDeactivate() {
    return Mono.fromRunnable(() -> ACTIVE_ACTOR.remove(super.getId().toString())).then(super.onDeactivate());
  }
}