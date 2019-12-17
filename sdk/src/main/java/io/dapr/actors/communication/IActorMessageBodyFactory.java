/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.communication;

/**
 * Defines the interface that must be implemented for providing factory for
 * creating actor request body and response body objects.
 *
 *
 * @author Swen Schisler <swen.schisler@fourtytwosoft.io>
 */
public interface IActorMessageBodyFactory {

  /**
   * Creates an actor request message body.
   *
   * @param interfaceName This is FullName for the service interface for which
   * request body is being constructed.
   * @param methodName MethodName for the service interface for which request
   * will be sent to.
   * @param numberOfParameters Number of Parameters in that Method.
   * @param wrappedRequestObject Wrapped Request Object.
   *
   * @return IActorRequestMessageBody
   */
  //TODO: exporting non public type through public API warning?
  IActorRequestMessageBody CreateRequestMessageBody(
      String interfaceName,
      String methodName,
      int numberOfParameters,
      Object wrappedRequestObject);

  /**
   * Creates an actor response message body.
   *
   * @param interfaceName This is FullName for the service interface for which
   * request body is being constructed.
   * @param methodName MethodName for the service interface for which request
   * will be sent to.
   * @param wrappedResponseObject Wrapped Response Object.
   *
   * @return IActorResponseMessageBody.
   */
  IActorResponseMessageBody CreateResponseMessageBody(
      String interfaceName,
      String methodName,
      Object wrappedResponseObject);
}
