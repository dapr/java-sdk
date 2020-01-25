/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import io.dapr.actors.runtime.ActorType;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

@ActorType(name = "DemoActorTest")
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
  protected Mono<Void> onActivate() {
    return Mono.fromRunnable(() -> ACTIVE_ACTOR.add(super.getId().toString())).then(super.onActivate());
  }

  @Override
  protected Mono<Void> onDeactivate() {
    return Mono.fromRunnable(() -> ACTIVE_ACTOR.remove(super.getId().toString())).then(super.onDeactivate());
  }
}