/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services.springboot;

public interface StatefulActor {

  void writeMessage(String something);

  String readMessage();

  void writeName(String something);

  String readName();

  void writeData(MyData something);

  MyData readData();

  void writeBytes(byte[] something);

  byte[] readBytes();

  class MyData {
    public String value;
  }
}
