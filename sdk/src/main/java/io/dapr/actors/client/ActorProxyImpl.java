package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implements a proxy client for an Actor's instance.
 */
class ActorProxyImpl implements ActorProxy {

  /**
   * EMPTY data for null response.
   */
  private static final byte[] EMPTY_DATA = new byte[0];

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
  private final ObjectSerializer serializer;

  /**
   * Client to talk to the Dapr's API.
   */
  private final ActorProxyAsyncClient daprClient;

  /**
   * Creates a new instance of {@link ActorProxyAsyncClient}.
   *
   * @param actorType  actor implementation type of the actor associated with the proxy object.
   * @param actorId    The actorId associated with the proxy
   * @param serializer Serializer and deserializer for method calls.
   * @param daprClient Dapr client.
   */
  ActorProxyImpl(String actorType, ActorId actorId, ObjectSerializer serializer, ActorProxyAsyncClient daprClient) {
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
    try {
      Mono<String> result = this.daprClient.invokeActorMethod(
        actorType,
        actorId.toString(),
        methodName,
        this.wrap(data));

      return result
        .filter(s -> (s != null) && (!s.isEmpty()))
        .map(s -> unwrap(s, clazz));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeActorMethod(String methodName, Class<T> clazz) {
    Mono<String> result = this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, null);
    return result
      .filter(s -> (s != null) && (!s.isEmpty()))
      .map(s -> unwrap(s, clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> invokeActorMethod(String methodName) {
    Mono<String> result = this.daprClient.invokeActorMethod(actorType, actorId.toString(), methodName, null);
    return result
      .filter(s -> (s != null) && (!s.isEmpty()))
      .map(s -> this.unwrap(s, String.class));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> invokeActorMethod(String methodName, Object data) {
    try {
      Mono<String> result = this.daprClient.invokeActorMethod(
        actorType,
        actorId.toString(),
        methodName,
        this.wrap(data));
      return result
        .filter(s -> (s != null) && (!s.isEmpty()))
        .map(s -> unwrap(s, String.class));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  /**
   * Extracts the response object from the Actor's method result.
   *
   * @param response String returned by API.
   * @param clazz    Expected response class.
   * @param <T>      Expected response type.
   * @return Response object, null or RuntimeException.
   */
  private <T> T unwrap(final String response, Class<T> clazz) {
    if (response == null) {
      return null;
    }

    try {
      ActorMethodEnvelope res = serializer.deserialize(response, ActorMethodEnvelope.class);
      if (res == null) {
        return null;
      }

      byte[] data = res.getData();
      if (data == null) {
        return null;
      }

      return this.serializer.deserialize(new String(data, StandardCharsets.UTF_8), clazz);
    } catch (IOException e) {
      // Wrap it to make Mono happy.
      throw new RuntimeException(e);
    }
  }

  /**
   * Builds the request to invoke an API for Actors.
   *
   * @param request Request object for the original Actor's method.
   * @param <T>     Type for the original Actor's method request.
   * @return String to be sent to Dapr's API.
   * @throws IOException In case it cannot generate String.
   */
  private <T> String wrap(final T request) throws IOException {
    if (request == null) {
      return null;
    }

    String json = this.serializer.serialize(request);
    ActorMethodEnvelope req = new ActorMethodEnvelope();
    req.setData(json == null ? EMPTY_DATA : json.getBytes());
    return serializer.serialize(req);
  }

}
