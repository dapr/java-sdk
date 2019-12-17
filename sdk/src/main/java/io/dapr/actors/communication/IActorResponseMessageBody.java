/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.communication;

import java.lang.reflect.Type;

/**
 * Defines the interface that must be implemented to provide Request Message
 * Body for remoting requests . This contains all the parameters remoting method
 * has.
 *
 */
public interface IActorResponseMessageBody {

  /**
   * Sets the response of a remoting Method in a remoting response Body.
   *
   * @param response Remoting Method Response.
   */
  void set(Object response);

  /**
   * Gets the response of a remoting Method from a remoting response body before
   * sending it to Client.
   *
   * @param paramType Return Type of a Remoting Method.
   * @return Remoting Method Response.
   */
  Object get(Type paramType);
}
