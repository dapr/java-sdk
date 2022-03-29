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
