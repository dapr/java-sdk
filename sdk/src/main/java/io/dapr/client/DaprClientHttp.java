/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.serializer.StringContentType;
import io.dapr.utils.Constants;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An adapter for the HTTP Client.
 *
 * @see io.dapr.client.DaprHttp
 * @see io.dapr.client.DaprClient
 */
public class DaprClientHttp implements DaprClient {

  /**
   * Serializer for internal objects.
   */
  private static final ObjectSerializer INTERNAL_SERIALIZER = new ObjectSerializer();

  /**
   * The HTTP client to be used.
   *
   * @see io.dapr.client.DaprHttp
   */
  private final DaprHttp client;

  /**
   * A utility class for serialize and deserialize customer's transient objects.
   */
  private final DaprObjectSerializer objectSerializer;

  /**
   * A utility class for serialize and deserialize customer's state objects.
   */
  private final DaprObjectSerializer stateSerializer;

  /**
   * Flag determining if serializer's input and output contains a valid String.
   */
  private final boolean isStateString;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param client           Dapr's http client.
   * @param objectSerializer Dapr's serializer for transient request/response objects.
   * @param stateSerializer  Dapr's serializer for state objects.
   * @see DaprClientBuilder
   * @see DefaultObjectSerializer
   */
  DaprClientHttp(DaprHttp client, DaprObjectSerializer objectSerializer, DaprObjectSerializer stateSerializer) {
    this.client = client;
    this.objectSerializer = objectSerializer;
    this.stateSerializer = stateSerializer;
    this.isStateString = stateSerializer.getClass().getAnnotation(StringContentType.class) != null;
  }

  /**
   * Constructor useful for tests.
   *
   * @param client Dapr's http client.
   * @see io.dapr.client.DaprClientBuilder
   * @see DefaultObjectSerializer
   */
  DaprClientHttp(DaprHttp client) {
    this(client, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object event) {
    return this.publishEvent(topic, event, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object event, Map<String, String> metadata) {
    try {
      if (topic == null || topic.trim().isEmpty()) {
        throw new IllegalArgumentException("Topic name cannot be null or empty.");
      }

      byte[] serializedEvent = objectSerializer.serialize(event);
      StringBuilder url = new StringBuilder(Constants.PUBLISH_PATH).append("/").append(topic);
      return this.client.invokeApi(
          DaprHttp.HttpMethods.POST.name(), url.toString(), null, serializedEvent, metadata).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      Verb verb, String appId, String method, Object request, Map<String, String> metadata, Class<T> clazz) {
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
      Mono<DaprHttp.Response> response = this.client.invokeApi(httMethod, path, metadata, serializedRequestBody, null);
      return response.flatMap(r -> {
        try {
          T object = objectSerializer.deserialize(r.getBody(), clazz);
          if (object == null) {
            return Mono.empty();
          }

          return Mono.just(object);
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
  public <T> Mono<T> invokeService(
      Verb verb, String appId, String method, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeService(verb, appId, method, null, metadata, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(Verb verb, String appId, String method, Object request, Class<T> clazz) {
    return this.invokeService(verb, appId, method, request, null, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(Verb verb, String appId, String method, Object request) {
    return this.invokeService(verb, appId, method, request, null, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      Verb verb, String appId, String method, Object request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      Verb verb, String appId, String method, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, null, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeService(
      Verb verb, String appId, String method, byte[] request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String name, Object request) {
    try {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Name to bind cannot be null or empty.");
      }

      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("data", request);
      StringBuilder url = new StringBuilder(Constants.BINDING_PATH).append("/").append(name);

      return this.client
          .invokeApi(
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
    return this.getState(state.getKey(), state.getEtag(), state.getOptions(), clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String key, Class<T> clazz) {
    return this.getState(key, null, null, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String key, String etag, StateOptions options, Class<T> clazz) {
    try {
      if (key == null) {
        throw new IllegalArgumentException("Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }

      StringBuilder url = new StringBuilder(Constants.STATE_PATH)
          .append("/")
          .append(key);
      Map<String, String> urlParameters = Optional.ofNullable(options)
          .map(o -> o.getStateOptionsAsMap())
          .orElse(new HashMap<>());

      return this.client
          .invokeApi(DaprHttp.HttpMethods.GET.name(), url.toString(), urlParameters, headers)
          .flatMap(s -> {
            try {
              return Mono.just(buildStateKeyValue(s, key, options, clazz));
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
  public Mono<Void> saveStates(List<State<?>> states) {
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
      List<State<Object>> internalStateObjects = new ArrayList<>(states.size());
      for (State state : states) {
        if (state == null) {
          continue;
        }
        byte[] data = this.stateSerializer.serialize(state.getValue());
        if (this.isStateString) {
          internalStateObjects.add(
              new State<>(data == null ? null : new String(data), state.getKey(), state.getEtag(), state.getOptions()));
        } else {
          internalStateObjects.add(new State<>(data, state.getKey(), state.getEtag(), state.getOptions()));
        }
      }
      byte[] serializedStateBody = INTERNAL_SERIALIZER.serialize(states);
      return this.client.invokeApi(
          DaprHttp.HttpMethods.POST.name(), url, null, serializedStateBody, headers).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String key, Object value) {
    return this.saveState(key, null, value, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String key, String etag, Object value, StateOptions options) {
    return Mono.fromSupplier(() -> new State<>(value, key, etag, options))
        .flatMap(state -> saveStates(Arrays.asList(state)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String key) {
    return this.deleteState(key, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String key, String etag, StateOptions options) {
    try {
      if (key == null || key.trim().isEmpty()) {
        throw new IllegalArgumentException("Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }
      String url = Constants.STATE_PATH + "/" + key;
      Map<String, String> urlParameters = Optional.ofNullable(options)
          .map(stateOptions -> stateOptions.getStateOptionsAsMap())
          .orElse(new HashMap<>());

      return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), url, urlParameters, headers).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * Builds a State object based on the Response.
   *
   * @param response     The response of the HTTP Call
   * @param requestedKey The Key Requested.
   * @param clazz        The Class of the Value of the state
   * @param <T>          The Type of the Value of the state
   * @return A StateKeyValue instance
   * @throws IOException If there's a issue deserialzing the response.
   */
  private <T> State<T> buildStateKeyValue(
      DaprHttp.Response response, String requestedKey, StateOptions stateOptions, Class<T> clazz) throws IOException {
    // The state is in the body directly, so we use the state serializer here.
    T value = stateSerializer.deserialize(response.getBody(), clazz);
    String key = requestedKey;
    String etag = null;
    if (response.getHeaders() != null && response.getHeaders().containsKey("Etag")) {
      etag = response.getHeaders().get("Etag");
    }
    return new State<>(value, key, etag, stateOptions);
  }

}
