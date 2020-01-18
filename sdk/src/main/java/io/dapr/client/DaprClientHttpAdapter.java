/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;

/**
 * An adapter for the HTTP Client.
 *
 * @see io.dapr.client.DaprHttp
 * @see io.dapr.client.DaprClient
 */
public class DaprClientHttpAdapter implements DaprClient {

  /**
   * Empty response.
   */
  private static final byte[] EMPTY_RAW_RESPONSE = new byte[0];

  /**
   * The HTTP client to be used
   *
   * @see io.dapr.client.DaprHttp
   */
  private final DaprHttp client;

  /**
   * A utitlity class for serialize and deserialize the messages sent and retrived by the client.
   */
  private final ObjectSerializer objectSerializer;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param client Dapr's http client.
   * @see io.dapr.client.DaprClientBuilder
   */
  DaprClientHttpAdapter(DaprHttp client) {
    this.client = client;
    this.objectSerializer = new ObjectSerializer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> publishEvent(String topic, T event) {
    return this.publishEvent(topic, event, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> publishEvent(String topic, T event, Map<String, String> metadata) {
    try {
      if (topic == null || topic.trim().isEmpty()) {
        throw new IllegalArgumentException("Topic name cannot be null or empty.");
      }

      byte[] serializedEvent = objectSerializer.serialize(event);
      StringBuilder url = new StringBuilder(Constants.PUBLISH_PATH).append("/").append(topic);
      return this.client.invokeAPI(
          DaprHttp.HttpMethods.POST.name(), url.toString(), null, serializedEvent, metadata).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, R> Mono<T> invokeService(Verb verb, String appId, String method, R request, Map<String, String> metadata, Class<T> clazz) {
    try {
      if (verb == null) {
        throw new IllegalArgumentException("Verb cannot be null.");
      }
      String httMethod = verb.toString();
      if (appId == null || appId.trim().isEmpty()) {
        throw new IllegalArgumentException("App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new IllegalArgumentException("Method name cannot be null or empty.");
      }
      String path = String.format("%s/%s/method/%s", Constants.INVOKE_PATH, appId, method);
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      Mono<DaprHttp.Response> response = this.client.invokeAPI(httMethod, path, null, serializedRequestBody, metadata);
      return response.flatMap(r -> {
            try {
              return Mono.just(objectSerializer.deserialize(r.getBody(), clazz));
            } catch (Exception ex) {
              return Mono.error(ex);
            }
          });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(Verb verb, String appId, String method, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeService(verb, appId, method, null, null, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <R> Mono<Void> invokeService(Verb verb, String appId, String method, R request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(Verb verb, String appId, String method, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, null, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeService(Verb verb, String appId, String method, byte[] request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> invokeBinding(String name, T request) {
    try {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Name to bind cannot be null or empty.");
      }

      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("data", request);
      StringBuilder url = new StringBuilder(Constants.BINDING_PATH).append("/").append(name);

      return this.client
          .invokeAPI(
              DaprHttp.HttpMethods.POST.name(),
              url.toString(),
              null,
              objectSerializer.serialize(jsonMap),
              null)
          .then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(State<T> state, Class<T> clazz) {
    try {
      if (state.getKey() == null) {
        throw new IllegalArgumentException("Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (state.getEtag() != null && !state.getEtag().trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, state.getEtag());
      }

      StringBuilder url = new StringBuilder(Constants.STATE_PATH)
        .append("/")
        .append(state.getKey());
      Map<String, String> urlParameters = Optional.ofNullable(state.getOptions()).map(options -> options.getStateOptionsAsMap() ).orElse( new HashMap<>());;
      return this.client
          .invokeAPI(DaprHttp.HttpMethods.GET.name(), url.toString(), urlParameters, headers)
          .flatMap(s -> {
            try {
              return Mono.just(buildStateKeyValue(s, state.getKey(), state.getOptions(), clazz));
            }catch (Exception ex){
              return Mono.error(ex);
            }
          });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> saveStates(List<State<T>> states) {
    try {
      if (states == null || states.isEmpty()) {
        return Mono.empty();
      }
      final Map<String, String> headers = new HashMap<>();
      final String etag = states.stream().filter(state -> null != state.getEtag() && !state.getEtag().trim().isEmpty())
          .findFirst().orElse(new State<>(null, null, null, null)).getEtag();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }
      final String url = Constants.STATE_PATH;
      byte[] serializedStateBody = objectSerializer.serialize(states);
      return this.client.invokeAPI(
        DaprHttp.HttpMethods.POST.name(), url, null, serializedStateBody, headers).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> saveState(String key, String etag, T value, StateOptions options) {
    State<T> state = new State<>(value, key, etag, options);
    return saveStates(Arrays.asList(state));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> deleteState(State<T> state) {
    try {
      if (state == null) {
        throw new IllegalArgumentException("State cannot be null.");
      }
      if (state.getKey() == null || state.getKey().trim().isEmpty()) {
        throw new IllegalArgumentException("Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (state.getEtag() != null && !state.getEtag().trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, state.getEtag());
      }
      String url = Constants.STATE_PATH + "/" + state.getKey();
      Map<String, String> urlParameters = Optional.ofNullable(state.getOptions()).map(stateOptions -> stateOptions.getStateOptionsAsMap()).orElse( new HashMap<>());;
      return this.client.invokeAPI(DaprHttp.HttpMethods.DELETE.name(), url, urlParameters, headers).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeActorMethod(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    Mono<DaprHttp.Response> responseMono = this.client.invokeAPI(DaprHttp.HttpMethods.POST.name(), url, null, jsonPayload, null);
    return responseMono.map(r -> emptyIfNull(r.getBody()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> getActorState(String actorType, String actorId, String keyName) {
    String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    Mono<DaprHttp.Response> responseMono = this.client.invokeAPI(DaprHttp.HttpMethods.GET.name(), url, null, "", null);
    return responseMono.map(r -> emptyIfNull(r.getBody()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveActorStateTransactionally(String actorType, String actorId, byte[] data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return this.client.invokeAPI(DaprHttp.HttpMethods.PUT.name(), url, null, data, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerActorReminder(String actorType, String actorId, String reminderName, byte[] data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.PUT.name(), url, null, data, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterActorReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.DELETE.name(), url, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerActorTimer(String actorType, String actorId, String timerName, byte[] data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.PUT.name(), url,null, data, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterActorTimer(String actorType, String actorId, String timerName) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.DELETE.name(), url, null, null).then();
  }

  /**
   * Builds a StateValue object based on the Response
   * @param response      The response of the HTTP Call
   * @param requestedKey The Key Requested.
   * @param clazz        The Class of the Value of the state
   * @param <T>          The Type of the Value of the state
   * @return             A StateKeyValue instance
   * @throws IOException If there's a issue deserialzing the response.
   */
  private <T> State<T> buildStateKeyValue(
    DaprHttp.Response response, String requestedKey, StateOptions stateOptions, Class<T> clazz) throws IOException {
    T value = objectSerializer.deserialize(response.getBody(), clazz);
    String key = requestedKey;
    String etag = null;
    if (response.getHeaders() != null && response.getHeaders().containsKey("Etag")) {
      etag = response.getHeaders().get("Etag");
    }
    return new State<>(value, key, etag, stateOptions);
  }

  /**
   * Returns an empty byte[] if payload is null.
   *
   * @param payload payload to be checked.
   * @return Non-null response with payload or empty array.
   */
  private static byte[] emptyIfNull(byte[] payload) {
    if (payload == null) {
      return EMPTY_RAW_RESPONSE;
    }

    return payload;
  }

}
