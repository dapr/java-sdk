/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

public interface StatefulActor {

  void writeMessage(String something);

  String readMessage();

  void writeData(MyData something);

  MyData readData();

  class MyData {
    public String value;
  }
}
