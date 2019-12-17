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
 * @author Swen Schisler <swen.schisler@fourtytwosoft.io>
 */
public interface IActorRequestMessageBody {

  /**
   * This Api gets called to set remoting method parameters before
   * serializing/dispatching the request.
   *
   * @param position Position of the parameter in Remoting Method.
   * @param parameName Parameter Name in the Remoting Method.
   * @param parameter Parameter Value.
   */
  void SetParameter(int position, String parameName, Object parameter);

  /**
   * This is used to retrieve parameter from request body before dispatching to
   * service remoting method.
   *
   * @param position Position of the parameter in Remoting Method.
   * @param parameName Parameter Name in the Remoting Method.
   * @param paramType Parameter Type.
   * @return The parameter that is at the specified position and has the
   * specified name.
   */
  Object GetParameter(int position, String parameName, Type paramType);

}
