/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.examples;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Dapr's Main Application to run using fatjar. This class will call the main source provided by user dynamically.
 */
public class DaprMainApplication {
  /**
   * Calls main method of the class provided by the user.
   * @param args  Accepts the classname as the first parameter. The rest are passed as argument as args.
   */
  public static void main(String[] args) throws Exception {
    String[] arguments;
    if (args.length < 1) {
      throw new IllegalArgumentException("Requires at least one argument - name of the main class");
    } else {
      arguments = Arrays.copyOfRange(args, 1, args.length);
      Class mainClass = Class.forName(args[0]);
      Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
      Object[] methodArgs = new Object[1];
      methodArgs[0] = arguments;
      mainMethod.invoke(mainClass, methodArgs);
    }
  }
}
