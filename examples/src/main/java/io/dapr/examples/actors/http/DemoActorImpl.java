/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors.http;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.*;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Implementation of the DemoActor for the server side.
 */
@ActorType(Name = "DemoActor")
public class DemoActorImpl extends AbstractActor implements DemoActor, Actor, Remindable<Integer> {

  /**
   * Format to output date and time.
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  public DemoActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);

    super.registerActorTimer(
      null,
      "clock",
      "ping!",
      Duration.ofSeconds(2),
      Duration.ofSeconds(1));
  }

  @Override
  public void registerReminder() {
    super.registerReminder(
      "myremind",
      (int)(Integer.MAX_VALUE * Math.random()),
      Duration.ofSeconds(5),
      Duration.ofSeconds(2));
  }

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
  public void clock(String message) {
    Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

    // Handles the request by printing message.
    System.out.println("Server timer for actor " +
      super.getId() + ": " +
      (message == null ? "" : message + " @ " + utcNowAsString));
  }

  @Override
  public Class<Integer> getStateType() {
    return Integer.class;
  }

  @Override
  public Mono<Void> receiveReminder(String reminderName, Integer state, Duration dueTime, Duration period) {
    Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

    // Handles the request by printing message.
    System.out.println(String.format(
      "Server reminded actor %s of: %s for %d @ %s",
      this.getId(), reminderName, state, utcNowAsString));
    return Mono.empty();
  }
}
