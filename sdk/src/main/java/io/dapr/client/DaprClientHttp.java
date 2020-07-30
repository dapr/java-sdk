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
import io.dapr.utils.Constants;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
   * Flag determining if object serializer's input and output is Dapr's default instead of user provided.
   */
  private final boolean isObjectSerializerDefault;

  /**
   * Flag determining if state serializer is the default serializer instead of user provided.
   */
  private final boolean isStateSerializerDefault;

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
    this.isObjectSerializerDefault = objectSerializer.getClass() == DefaultObjectSerializer.class;
    this.isStateSerializerDefault = stateSerializer.getClass() == DefaultObjectSerializer.class;
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
  public Mono<Void> publishEvent(String topic, Object data) {
    return this.publishEvent(topic, data, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object data, Map<String, String> metadata) {
    try {
      if (topic == null || topic.trim().isEmpty()) {
        throw new IllegalArgumentException("Topic name cannot be null or empty.");
      }

      byte[] serializedEvent = objectSerializer.serialize(data);
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
  public <T> Mono<T> invokeService(
      String appId,
      String method,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      TypeRef<T> type) {
    try {
      if (httpExtension == null) {
        throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
      }
      // If the httpExtension is not null, then the method will not be null based on checks in constructor
      String httMethod = httpExtension.getMethod().toString();
      if (appId == null || appId.trim().isEmpty()) {
        throw new IllegalArgumentException("App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new IllegalArgumentException("Method name cannot be null or empty.");
      }
      String path = String.format("%s/%s/method/%s", Constants.INVOKE_PATH, appId, method);
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      Mono<DaprHttp.Response> response = this.client.invokeApi(httMethod, path,
          httpExtension.getQueryString(), serializedRequestBody, metadata);
      return response.flatMap(r -> {
        try {
          T object = objectSerializer.deserialize(r.getBody(), type);
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
      String appId,
      String method,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      Class<T> clazz) {
    return this.invokeService(appId, method, request, httpExtension, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      String appId, String method, HttpExtension httpExtension, Map<String, String> metadata, TypeRef<T> type) {
    return this.invokeService(appId, method, null, httpExtension, metadata, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      String appId, String method, HttpExtension httpExtension, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeService(appId, method, null, httpExtension, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                                   TypeRef<T> type) {
    return this.invokeService(appId, method, request, httpExtension, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                                   Class<T> clazz) {
    return this.invokeService(appId, method, request, httpExtension,null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(String appId, String method, Object request, HttpExtension httpExtension) {
    return this.invokeService(appId, method, request, httpExtension, null, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      String appId, String method, Object request, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeService(appId, method, request, httpExtension, metadata, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      String appId, String method, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeService(appId, method, null, httpExtension, metadata, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeService(
      String appId, String method, byte[] request, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeService(appId, method, request, httpExtension, metadata, TypeRef.BYTE_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String name, String operation, Object data) {
    return this.invokeBinding(name, operation, data, null, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeBinding(String name, String operation, byte[] data, Map<String, String> metadata) {
    return this.invokeBinding(name, operation, data, metadata, TypeRef.BYTE_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String name, String operation, Object data, TypeRef<T> type) {
    return this.invokeBinding(name, operation, data, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String name, String operation, Object data, Class<T> clazz) {
    return this.invokeBinding(name, operation, data, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(
      String name, String operation, Object data, Map<String, String> metadata, TypeRef<T> type) {
    try {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Binding name cannot be null or empty.");
      }

      if (operation == null || operation.trim().isEmpty()) {
        throw new IllegalArgumentException("Binding operation cannot be null or empty.");
      }

      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("operation", operation);
      if (metadata != null) {
        jsonMap.put("metadata", metadata);
      }

      if (data != null) {
        if (this.isObjectSerializerDefault) {
          // If we are using Dapr's default serializer, we pass the object directly and skip objectSerializer.
          // This allows binding to receive JSON directly without having to extract it from a quoted string.
          // Example of output binding vs body in the input binding:
          //   This logic DOES this:
          //     Output Binding: { "data" : { "mykey": "myvalue" } }
          //     Input Binding: { "mykey": "myvalue" }
          //   This logic AVOIDS this:
          //     Output Binding: { "data" : "{ \"mykey\": \"myvalue\" }" }
          //     Input Binding: "{ \"mykey\": \"myvalue\" }"
          jsonMap.put("data", data);
        } else {
          // When customer provides a custom serializer, he will get a Base64 encoded String back - always.
          // Example of body in the input binding resulting from this logic:
          //   { "data" : "eyJrZXkiOiAidmFsdWUifQ==" }
          jsonMap.put("data", objectSerializer.serialize(data));
        }
      }

      StringBuilder url = new StringBuilder(Constants.BINDING_PATH).append("/").append(name);

      byte[] payload = INTERNAL_SERIALIZER.serialize(jsonMap);
      String httpMethod = DaprHttp.HttpMethods.POST.name();
      Mono<DaprHttp.Response> response = this.client.invokeApi(httpMethod, url.toString(), null, payload, null);
      return response.flatMap(r -> {
        try {
          T object = objectSerializer.deserialize(r.getBody(), type);
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
  public <T> Mono<T> invokeBinding(
      String name, String operation, Object data, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeBinding(name, operation, data, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, State<T> state, TypeRef<T> type) {
    return this.getState(stateStoreName, state.getKey(), state.getEtag(), state.getOptions(), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, State<T> state, Class<T> clazz) {
    return this.getState(stateStoreName, state.getKey(), state.getEtag(), state.getOptions(), TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, String key, TypeRef<T> type) {
    return this.getState(stateStoreName, key, null, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, String key, Class<T> clazz) {
    return this.getState(stateStoreName, key, null, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(
      String stateStoreName, String key, String etag, StateOptions options, TypeRef<T> type) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }

      StringBuilder url = new StringBuilder(Constants.STATE_PATH)
          .append("/")
          .append(stateStoreName)
          .append("/")
          .append(key);
      Map<String, String> urlParameters = Optional.ofNullable(options)
          .map(o -> o.getStateOptionsAsMap())
          .orElse(new HashMap<>());

      return this.client
          .invokeApi(DaprHttp.HttpMethods.GET.name(), url.toString(), urlParameters, headers)
          .flatMap(s -> {
            try {
              return Mono.just(buildStateKeyValue(s, key, options, type));
            } catch (Exception ex) {
              return Mono.error(ex);
            }
          });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T> Mono<State<T>> getState(
      String stateStoreName, String key, String etag, StateOptions options, Class<T> clazz) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStates(String stateStoreName, List<State<?>> states) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (states == null || states.isEmpty()) {
        return Mono.empty();
      }
      final Map<String, String> headers = new HashMap<>();
      final String etag = states.stream().filter(state -> null != state.getEtag() && !state.getEtag().trim().isEmpty())
          .findFirst().orElse(new State<>(null, null, null, null)).getEtag();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }
      final String url = Constants.STATE_PATH + "/" + stateStoreName;
      List<State<Object>> internalStateObjects = new ArrayList<>(states.size());
      for (State state : states) {
        if (state == null) {
          continue;
        }
        if (this.isStateSerializerDefault) {
          // If default serializer is being used, we just pass the object through to be serialized directly.
          // This avoids a JSON object from being quoted inside a string.
          // We WANT this: { "value" : { "myField" : 123 } }
          // We DON't WANT this: { "value" : "{ \"myField\" : 123 }" }
          internalStateObjects.add(state);
          continue;
        }

        byte[] data = this.stateSerializer.serialize(state.getValue());
        // Custom serializer, so everything is byte[].
        internalStateObjects.add(new State<>(data, state.getKey(), state.getEtag(), state.getOptions()));
      }
      byte[] serializedStateBody = INTERNAL_SERIALIZER.serialize(internalStateObjects);
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
  public Mono<Void> saveState(String stateStoreName, String key, Object value) {
    return this.saveState(stateStoreName, key, null, value, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String stateStoreName, String key, String etag, Object value, StateOptions options) {
    return Mono.fromSupplier(() -> new State<>(value, key, etag, options))
        .flatMap(state -> saveStates(stateStoreName, Arrays.asList(state)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String stateStoreName, String key) {
    return this.deleteState(stateStoreName, key, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String stateStoreName, String key, String etag, StateOptions options) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }
      String url = Constants.STATE_PATH + "/" + stateStoreName + "/" + key;
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
   * @param type        The Class of the Value of the state
   * @param <T>          The Type of the Value of the state
   * @return A StateKeyValue instance
   * @throws IOException If there's a issue deserialzing the response.
   */
  private <T> State<T> buildStateKeyValue(
      DaprHttp.Response response, String requestedKey, StateOptions stateOptions, TypeRef<T> type) throws IOException {
    // The state is in the body directly, so we use the state serializer here.
    T value = stateSerializer.deserialize(response.getBody(), type);
    String key = requestedKey;
    String etag = null;
    if (response.getHeaders() != null && response.getHeaders().containsKey("Etag")) {
      etag = response.getHeaders().get("Etag");
    }
    return new State<>(value, key, etag, stateOptions);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String secretStoreName, String secretName, Map<String, String> metadata) {
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((secretName == null) || (secretName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret name cannot be null or empty.");
      }
    } catch (Exception e) {
      return Mono.error(e);
    }

    String url = Constants.SECRETS_PATH + "/" + secretStoreName + "/" + secretName;
    return this.client
      .invokeApi(DaprHttp.HttpMethods.GET.name(), url, metadata, (String)null, null)
      .flatMap(response -> {
        try {
          Map m =  INTERNAL_SERIALIZER.deserialize(response.getBody(), Map.class);
          if (m == null) {
            return Mono.just(Collections.EMPTY_MAP);
          }

          return Mono.just(m);
        } catch (IOException e) {
          return Mono.error(e);
        }
      })
      .map(m -> (Map<String, String>)m);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String secretStoreName, String secretName) {
    return this.getSecret(secretStoreName, secretName, null);
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}
