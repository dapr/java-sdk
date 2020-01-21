/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors.http;

import reactor.core.publisher.Mono;

/**
 * Example of implementation of an Actor.
 */
public interface DemoActor {

  void registerReminder();

  String say(String something);

  void clock(String message);

  Mono<Integer> incrementAndGet(int delta);
}
