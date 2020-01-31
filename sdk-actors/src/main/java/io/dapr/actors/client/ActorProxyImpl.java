package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.runtime.ObjectSerializer;
import io.dapr.serializer.DaprObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Implements a proxy client for an Actor's instance.
 */
class ActorProxyImpl implements ActorProxy {

  /**
   * Serializer used for internal objects.
   */
  private static final ObjectSerializer INTERNAL_SERIALIZER = new ObjectSerializer();

  /**
   * Actor's identifier for this Actor instance.
   */
  private final ActorId actorId;

  /**
   * Actor's type for this Actor instance.
   */
  private final String actorType;

  /**
   * Serializer/deserialzier to exchange message for Actors.
   */
  private final DaprObjectSerializer serializer;

  /**
   * Client to talk to the Dapr's API.
   */
  private final DaprClient daprClient;

  /**
   * Creates a new instance of {@link ActorProxyImpl}.
   *
   * @param actorType  actor implementation type of the actor associated with the proxy object.
   * @param actorId    The actorId associated with the proxy
   * @param serializer Serializer and deserializer for method calls.
   * @param daprClient Dapr client.
   */
  ActorProxyImpl(String actorType, ActorId actorId, DaprObjectSerializer serializer, DaprClient daprClient) {
    this.actorType = actorType;
    this.actorId = actorId;
    this.daprClient = daprClient;
    this.serializer = serializer;
  }

  /**
   * {@inheritDoc}
   */
  public ActorId getActorId() {
    return actorId;
  }

  /**
   * {@inheritDoc}
   */
  public String getActorType() {
    return actorType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeActorMethod(String methodName, Object data, Class<T> clazz) {
    return this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, this.wrap(data))
          .filter(s -> s.length > 0)
          .map(s -> unwrap(s, clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeActorMethod(String methodName, Class<T> clazz) {
    return this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, null)
          .filter(s -> s.length > 0)
          .map(s -> unwrap(s, clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeActorMethod(String methodName) {
    return this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeActorMethod(String methodName, Object data) {
    return this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, this.wrap(data)).then();
  }

  /**
   * Extracts the response object from the Actor's method result.
   *
   * @param response response returned by API.
   * @param clazz    Expected response class.
   * @param <T>      Expected response type.
   * @return Response object or null.
   * @throws RuntimeException In case it cannot generate Object.
   */
  private <T> T unwrap(final byte[] response, Class<T> clazz) {
    try {
      return this.serializer.deserialize(INTERNAL_SERIALIZER.unwrapData(response), clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds the request to invoke an API for Actors.
   *
   * @param request Request object for the original Actor's method.
   * @return Payload to be sent to Dapr's API.
   * @throws RuntimeException In case it cannot generate payload.
   */
  private byte[] wrap(final Object request) {
    try {
      return INTERNAL_SERIALIZER.wrapData(this.serializer.serialize(request));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
