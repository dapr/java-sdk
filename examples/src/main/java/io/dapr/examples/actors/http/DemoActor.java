/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors.http;

/**
 * Example of implementation of an Actor.
 */
public interface DemoActor {

  String say(String something);
}
