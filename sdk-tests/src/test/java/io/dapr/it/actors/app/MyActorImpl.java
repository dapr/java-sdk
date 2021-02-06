/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.app;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.AbstractActor;
import io.dapr.actors.runtime.ActorRuntimeContext;
import io.dapr.actors.runtime.Remindable;
import io.dapr.it.actors.MethodEntryTracker;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

@ActorType(name = "MyActorTest")
public class MyActorImpl extends AbstractActor implements MyActor, Remindable<String> {
  private final String TIMER_CALLBACK_METHOD = "clock";

  public static final List<String> ACTIVE_ACTOR = new ArrayList<>();

  // this tracks method entries and is used to validate turn-based concurrency.
  public ArrayList<MethodEntryTracker> callLog = new ArrayList<MethodEntryTracker>();

  public MyActorImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id);

    this.callLog = new ArrayList<MethodEntryTracker>();
  }

  /**
   * Format to output date and time.
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  @Override
  public String say(String something) {
    String reversedString = "";
    try {
      this.formatAndLog(true, "say");
      reversedString = new StringBuilder(something).reverse().toString();

      this.formatAndLog(false, "say");
    } catch(Exception e) {
      // We don't throw, but the proxy side will know it failed because it expects a reversed input
      System.out.println("Caught " + e);
    }

    return reversedString;
  }

  @Override
  public List<String> retrieveActiveActors() {
    return Collections.unmodifiableList(ACTIVE_ACTOR);
  }

  @Override
  public void startReminder(String name) throws IllegalArgumentException {
    this.formatAndLog(true, "startReminder");
    try {
      super.registerReminder(
        name,
        "36",
        Duration.ofSeconds(1),
        Duration.ofSeconds(2)).block();
    } catch(Exception e) {
      // We don't throw, but the proxy side will know it failed because the reminder won't fire later
      System.out.println("Caught " + e);
    }

    this.formatAndLog(false, "startReminder");
  }

  @Override
  public void stopReminder(String name) {
    this.formatAndLog(true, "stopReminder");
    System.out.println("Enter stopReminder");
    super.unregisterReminder(name).block();
    this.formatAndLog(false, "stopReminder");
  }

  @Override
  public void startTimer(String name) {
    this.formatAndLog(true, "startTimer");
    System.out.println("Enter startTimer with timer name " + name);
    try {
      super.registerActorTimer(
        name,
        TIMER_CALLBACK_METHOD,
        "ping!",
        Duration.ofSeconds(2),
        Duration.ofSeconds(3)).block();
    } catch (Exception e) {
      // We don't throw, but the proxy side will know it failed because the test looks for the timer to fire later
      System.out.println("startTimer caught " + e);
    }
    this.formatAndLog(false, "startTimer");
  }

  @Override
  public void stopTimer(String name) {
    this.formatAndLog(true, "stopTimer");
    System.out.println("Enter stopTimer with timer name " + name);
    try {
      super.unregisterTimer(name).block();
    } catch (Exception e) {
      // We don't throw, but the proxy side will know it failed because the test validates the timer stops firing
      System.out.println("stopTimer caught " + e);
    }

    this.formatAndLog(false, "stopTimer");
  }

  @Override
  protected Mono<Void> onActivate() {
    return Mono.fromRunnable(() -> ACTIVE_ACTOR.add(super.getId().toString())).then(super.onActivate());
  }

  @Override
  protected Mono<Void> onDeactivate() {
    return Mono.fromRunnable(() -> ACTIVE_ACTOR.remove(super.getId().toString())).then(super.onDeactivate());
  }

  @Override
  public Mono<Void> receiveReminder(String reminderName, String state, Duration dueTime, Duration period) {
    return Mono.fromRunnable(() -> {
      this.formatAndLog(true, "receiveReminder");
      Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

      // Handles the request by printing message.
      System.out.println(String.format(
          "> Server reminded actor %s of: %s for %s @ %s hosted by instance id %s",
          this.getId(), reminderName, state, utcNowAsString, System.getenv("DAPR_HTTP_PORT")));

      this.formatAndLog(false, "receiveReminder");
    });
  }

  @Override
  public TypeRef<String> getStateType() {
    return TypeRef.STRING;
  }


  @Override
  public void clock(String message) {
    this.formatAndLog(true, TIMER_CALLBACK_METHOD);

    Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

    // Handles the request by printing message.
    String msg = ("Server timer for actor " +
      super.getId() + ": " +
      (message == null ? "" : message + " @ " + utcNowAsString));

    this.formatAndLog(false, TIMER_CALLBACK_METHOD);
  }

  @Override
  public ArrayList<String> getCallLog() {
    System.out.println("Enter getCallLog, size is " + this.callLog.size());

    ArrayList<String> stringList = new ArrayList<String>();
    try {
      for (MethodEntryTracker m : this.callLog) {
        String s = m.getIsEnter() ? "Enter" : "Exit";
        s += "|" + m.getMethodName();
        s += "|" + m.getDate().toString();
        stringList.add(s);
      }
    } catch(Exception e) {
      System.out.println("Caught " + e);
      return new ArrayList<String>();
    }

    return stringList;
  }

  /*
    Return an identifier so we can tell the difference between apps hosting the actor if it moves.
    Here we use the dapr http port.  Process id would be better but the available approaches
    do not appear portable.
   */
  @Override
  public String getIdentifier() {
    System.out.println("Enter getIdentifier");
    return System.getenv("DAPR_HTTP_PORT");
  }

  @Override
  public boolean dotNetMethod() {
    return true;
  }

  private void formatAndLog(boolean isEnter, String methodName) {
    Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    MethodEntryTracker entry = new MethodEntryTracker(isEnter, methodName, utcNow.getTime());
    this.callLog.add(entry);
  }
}