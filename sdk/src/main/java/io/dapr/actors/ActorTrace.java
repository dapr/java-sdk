/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors;

/**
 * Stub
 */
public class ActorTrace {

  public static void WriteInfo(String text) {
    System.out.println(text);
  }

  public static void WriteWarning(String text) {
    System.out.println("Warning: " + text);
  }

  public static void WriteError(String text) {
    System.err.println(text);
  }
}
