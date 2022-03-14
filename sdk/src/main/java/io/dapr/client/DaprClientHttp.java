/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.TransactionalStateRequest;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.NetworkUtils;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * An adapter for the HTTP Client.
 *
 * @see io.dapr.client.DaprHttp
 * @see io.dapr.client.DaprClient
 */
public class DaprClientHttp extends AbstractDaprClient {
  /**
   * Header for the conditional operation.
   */
  private static final String HEADER_HTTP_ETAG_ID = "If-Match";

  /**
   * Metadata prefix in query params.
   */
  private static final String METADATA_PREFIX = "metadata.";

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
    super(objectSerializer, stateSerializer);
    this.client = client;
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
  public Mono<Void> waitForSidecar(int timeoutInMilliseconds) {
    return Mono.fromRunnable(() -> {
      try {
        NetworkUtils.waitForSocket(Properties.SIDECAR_IP.get(), Properties.HTTP_PORT.get(), timeoutInMilliseconds);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(PublishEventRequest request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      Object data = request.getData();
      Map<String, String> metadata = request.getMetadata();

      if (topic == null || topic.trim().isEmpty()) {
        throw new IllegalArgumentException("Topic name cannot be null or empty.");
      }

      byte[] serializedEvent = objectSerializer.serialize(data);
      // Content-type can be overwritten on a per-request basis.
      // It allows CloudEvents to be handled differently, for example.
      String contentType = request.getContentType();
      if (contentType == null || contentType.isEmpty()) {
        contentType = objectSerializer.getContentType();
      }
      Map<String, String> headers = Collections.singletonMap("content-type", contentType);

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "publish", pubsubName, topic };

      Map<String, List<String>> queryArgs = metadataToQueryArgs(metadata);
      return Mono.subscriberContext().flatMap(
          context -> this.client.invokeApi(
              DaprHttp.HttpMethods.POST.name(), pathSegments, queryArgs, serializedEvent, headers, context
          )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    try {
      final String appId = invokeMethodRequest.getAppId();
      final String method = invokeMethodRequest.getMethod();
      final Object request = invokeMethodRequest.getBody();
      final HttpExtension httpExtension = invokeMethodRequest.getHttpExtension();
      final String contentType = invokeMethodRequest.getContentType();
      if (httpExtension == null) {
        throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
      }
      // If the httpExtension is not null, then the method will not be null based on checks in constructor
      final String httpMethod = httpExtension.getMethod().toString();
      if (appId == null || appId.trim().isEmpty()) {
        throw new IllegalArgumentException("App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new IllegalArgumentException("Method name cannot be null or empty.");
      }


      String[] methodSegments = method.split("/");

      List<String> pathSegments = new ArrayList<>(Arrays.asList(DaprHttp.API_VERSION, "invoke", appId, "method"));
      pathSegments.addAll(Arrays.asList(methodSegments));

      byte[] serializedRequestBody = objectSerializer.serialize(request);
      final Map<String, String> headers = new HashMap<>();
      if (contentType != null && !contentType.isEmpty()) {
        headers.put("content-type", contentType);
      }
      headers.putAll(httpExtension.getHeaders());
      Mono<DaprHttp.Response> response = Mono.subscriberContext().flatMap(
          context -> this.client.invokeApi(httpMethod, pathSegments.toArray(new String[0]),
              httpExtension.getQueryParams(), serializedRequestBody, headers, context)
      );
      return response.flatMap(r -> getMono(type, r));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private <T> Mono<T> getMono(TypeRef<T> type, DaprHttp.Response r) {
    try {
      T object = objectSerializer.deserialize(r.getBody(), type);
      if (object == null) {
        return Mono.empty();
      }

      return Mono.just(object);
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
    try {
      final String name = request.getName();
      final String operation = request.getOperation();
      final Object data = request.getData();
      final Map<String, String> metadata = request.getMetadata();
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

      byte[] payload = INTERNAL_SERIALIZER.serialize(jsonMap);
      String httpMethod = DaprHttp.HttpMethods.POST.name();

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "bindings", name };

      Mono<DaprHttp.Response> response = Mono.subscriberContext().flatMap(
          context -> this.client.invokeApi(
              httpMethod, pathSegments, null, payload, null, context)
      );
      return response.flatMap(r -> getMono(type, r));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<String> keys = request.getKeys();
      final int parallelism = request.getParallelism();
      final Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (keys == null || keys.isEmpty()) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      if (parallelism < 0) {
        throw new IllegalArgumentException("Parallelism cannot be negative.");
      }

      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("keys", keys);
      jsonMap.put("parallelism", parallelism);

      byte[] requestBody = INTERNAL_SERIALIZER.serialize(jsonMap);

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "state", stateStoreName, "bulk" };

      Map<String, List<String>> queryArgs = metadataToQueryArgs(metadata);
      return Mono.subscriberContext().flatMap(
          context -> this.client
              .invokeApi(DaprHttp.HttpMethods.POST.name(), pathSegments, queryArgs, requestBody, null, context)
      ).flatMap(s -> {
        try {
          return Mono.just(buildStates(s, type));
        } catch (Exception ex) {
          return DaprException.wrapMono(ex);
        }
      });

    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(GetStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final Map<String, String> metadata = request.getMetadata();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      Map<String, String> optionsMap = Optional.ofNullable(options)
          .map(o -> o.getStateOptionsAsMap())
          .orElse(Collections.emptyMap());

      final Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.putAll(metadataToQueryArgs(metadata));
      queryParams.putAll(optionsMap.entrySet().stream().collect(
          Collectors.toMap(kv -> kv.getKey(), kv -> Collections.singletonList(kv.getValue()))));

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "state", stateStoreName, key };

      return Mono.subscriberContext().flatMap(
          context -> this.client
              .invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments, queryParams, null, context)
      ).flatMap(s -> {
        try {
          return Mono.justOrEmpty(buildState(s, key, options, type));
        } catch (Exception ex) {
          return DaprException.wrapMono(ex);
        }
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<TransactionalStateOperation<?>> operations = request.getOperations();
      final Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (operations == null || operations.isEmpty()) {
        return Mono.empty();
      }

      List<TransactionalStateOperation<Object>> internalOperationObjects = new ArrayList<>(operations.size());
      for (TransactionalStateOperation operation : operations) {
        if (operation == null) {
          continue;
        }
        State<?> state = operation.getRequest();
        if (state == null) {
          continue;
        }
        if (this.isStateSerializerDefault) {
          // If default serializer is being used, we just pass the object through to be serialized directly.
          // This avoids a JSON object from being quoted inside a string.
          // We WANT this: { "value" : { "myField" : 123 } }
          // We DON't WANT this: { "value" : "{ \"myField\" : 123 }" }
          internalOperationObjects.add(operation);
          continue;
        }
        byte[] data = this.stateSerializer.serialize(state.getValue());
        // Custom serializer, so everything is byte[].
        internalOperationObjects.add(new TransactionalStateOperation<>(operation.getOperation(),
            new State<>(state.getKey(), data, state.getEtag(), state.getMetadata(), state.getOptions())));
      }
      TransactionalStateRequest<Object> req = new TransactionalStateRequest<>(internalOperationObjects, metadata);
      byte[] serializedOperationBody = INTERNAL_SERIALIZER.serialize(req);

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "state", stateStoreName, "transaction" };

      return Mono.subscriberContext().flatMap(
          context -> this.client.invokeApi(
              DaprHttp.HttpMethods.POST.name(), pathSegments, null, serializedOperationBody, null, context
          )
      ).then();
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(SaveStateRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final List<State<?>> states = request.getStates();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (states == null || states.isEmpty()) {
        return Mono.empty();
      }

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
        internalStateObjects.add(new State<>(state.getKey(), data, state.getEtag(), state.getMetadata(),
            state.getOptions()));
      }
      byte[] serializedStateBody = INTERNAL_SERIALIZER.serialize(internalStateObjects);

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "state", stateStoreName };

      return Mono.subscriberContext().flatMap(
          context -> this.client.invokeApi(
              DaprHttp.HttpMethods.POST.name(), pathSegments, null, serializedStateBody, null, context)
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(DeleteStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Map<String, String> metadata = request.getMetadata();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(HEADER_HTTP_ETAG_ID, etag);
      }

      Map<String, String> optionsMap = Optional.ofNullable(options)
          .map(o -> o.getStateOptionsAsMap())
          .orElse(Collections.emptyMap());

      final Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.putAll(metadataToQueryArgs(metadata));
      queryParams.putAll(optionsMap.entrySet().stream().collect(
          Collectors.toMap(kv -> kv.getKey(), kv -> Collections.singletonList(kv.getValue()))));

      String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "state", stateStoreName, key };

      return Mono.subscriberContext().flatMap(
          context -> this.client.invokeApi(
              DaprHttp.HttpMethods.DELETE.name(), pathSegments, queryParams, headers, context)
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  /**
   * Builds a State object based on the Response.
   *
   * @param response     The response of the HTTP Call
   * @param requestedKey The Key Requested.
   * @param type         The Class of the Value of the state
   * @param <T>          The Type of the Value of the state
   * @return A State instance
   * @throws IOException If there's a issue deserializing the response.
   */
  private <T> State<T> buildState(
      DaprHttp.Response response, String requestedKey, StateOptions stateOptions, TypeRef<T> type) throws IOException {
    // The state is in the body directly, so we use the state serializer here.
    T value = stateSerializer.deserialize(response.getBody(), type);
    String etag = null;
    if (response.getHeaders() != null && response.getHeaders().containsKey("Etag")) {
      etag = response.getHeaders().get("Etag");
    }
    return new State<>(requestedKey, value, etag, Collections.emptyMap(), stateOptions);
  }

  /**
   * Builds a State object based on the Response.
   *
   * @param response The response of the HTTP Call
   * @param type     The Class of the Value of the state
   * @param <T>      The Type of the Value of the state
   * @return A list of states.
   * @throws IOException If there's a issue deserializing the response.
   */
  private <T> List<State<T>> buildStates(
      DaprHttp.Response response, TypeRef<T> type) throws IOException {
    JsonNode root = INTERNAL_SERIALIZER.parseNode(response.getBody());
    List<State<T>> result = new ArrayList<>();
    for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
      JsonNode node = it.next();
      String key = node.path("key").asText();
      String error = node.path("error").asText();
      if (!Strings.isNullOrEmpty(error)) {
        result.add(new State<>(key, error));
        continue;
      }

      String etag = node.path("etag").asText();
      if (etag.equals("")) {
        etag = null;
      }
      // TODO(artursouza): JSON cannot differentiate if data returned is String or byte[], it is ambiguous.
      // This is not a high priority since GRPC is the default (and recommended) client implementation.
      byte[] data = node.path("data").toString().getBytes(Properties.STRING_CHARSET.get());
      T value = stateSerializer.deserialize(data, type);
      result.add(new State<>(key, value, etag));
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(GetSecretRequest request) {
    String secretStoreName = request.getStoreName();
    String key = request.getKey();
    Map<String, String> metadata = request.getMetadata();
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret key cannot be null or empty.");
      }
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }

    Map<String, List<String>> queryArgs = metadataToQueryArgs(metadata);
    String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "secrets", secretStoreName, key };

    return Mono.subscriberContext().flatMap(
            context -> this.client
                .invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments, queryArgs, (String) null, null, context)
        ).flatMap(response -> {
          try {
            Map m = INTERNAL_SERIALIZER.deserialize(response.getBody(), Map.class);
            if (m == null) {
              return Mono.just(Collections.EMPTY_MAP);
            }

            return Mono.just(m);
          } catch (IOException e) {
            return DaprException.wrapMono(e);
          }
        })
        .map(m -> (Map<String, String>) m);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request) {
    String secretStoreName = request.getStoreName();
    Map<String, String> metadata = request.getMetadata();
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }

    Map<String, List<String>> queryArgs = metadataToQueryArgs(metadata);
    String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "secrets", secretStoreName, "bulk" };

    return Mono.subscriberContext().flatMap(
            context -> this.client
                .invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments, queryArgs, (String) null, null, context)
        ).flatMap(response -> {
          try {
            Map m = INTERNAL_SERIALIZER.deserialize(response.getBody(), Map.class);
            if (m == null) {
              return Mono.just(Collections.EMPTY_MAP);
            }

            return Mono.just(m);
          } catch (IOException e) {
            return DaprException.wrapMono(e);
          }
        })
        .map(m -> (Map<String, Map<String, String>>) m);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, TypeRef<T> type) {
    try {
      if (request == null) {
        throw new IllegalArgumentException("Query state request cannot be null.");
      }
      String stateStoreName = request.getStoreName();
      Map<String, String> metadata = request.getMetadata();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      Map<String, List<String>> queryArgs = metadataToQueryArgs(metadata);
      String[] pathSegments = new String[]{ DaprHttp.ALPHA_1_API_VERSION, "state", stateStoreName, "query" };
      String serializedRequest;
      if (request.getQuery() != null) {
        serializedRequest = JSON_REQUEST_MAPPER.writeValueAsString(request.getQuery());
      } else if (request.getQueryString() != null) {
        serializedRequest = request.getQueryString();
      } else {
        throw new IllegalArgumentException("Both query and queryString fields are not set.");
      }
      return Mono.subscriberContext().flatMap(
              context -> this.client
                  .invokeApi(DaprHttp.HttpMethods.POST.name(), pathSegments,
                      queryArgs, serializedRequest, null, context)
          ).flatMap(response -> {
            try {
              return Mono.justOrEmpty(buildQueryStateResponse(response, type));
            } catch (Exception e) {
              return DaprException.wrapMono(e);
            }
          });
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    client.close();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> shutdown() {
    String[] pathSegments = new String[]{ DaprHttp.API_VERSION, "shutdown" };
    return Mono.subscriberContext().flatMap(
            context -> client.invokeApi(DaprHttp.HttpMethods.POST.name(), pathSegments,
                null, null, context))
        .then();
  }

  private <T> QueryStateResponse<T> buildQueryStateResponse(DaprHttp.Response response,
                                                            TypeRef<T> type) throws IOException {
    JsonNode root = INTERNAL_SERIALIZER.parseNode(response.getBody());
    if (!root.has("results")) {
      return new QueryStateResponse<>(Collections.emptyList(), null);
    }
    String token = null;
    if (root.has("token")) {
      token = root.path("token").asText();
    }
    Map<String, String> metadata = new HashMap<>();
    if (root.has("metadata")) {
      for (Iterator<Map.Entry<String, JsonNode>> it = root.get("metadata").fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = it.next();
        metadata.put(entry.getKey(), entry.getValue().asText());
      }
    }
    List<QueryStateItem<T>> result = new ArrayList<>();
    for (Iterator<JsonNode> it = root.get("results").elements(); it.hasNext(); ) {
      JsonNode node = it.next();
      String key = node.path("key").asText();
      String error = node.path("error").asText();
      if (!Strings.isNullOrEmpty(error)) {
        result.add(new QueryStateItem<>(key, null, error));
        continue;
      }

      String etag = node.path("etag").asText();
      if (etag.equals("")) {
        etag = null;
      }
      // TODO(artursouza): JSON cannot differentiate if data returned is String or byte[], it is ambiguous.
      // This is not a high priority since GRPC is the default (and recommended) client implementation.
      byte[] data = node.path("data").toString().getBytes(Properties.STRING_CHARSET.get());
      T value = stateSerializer.deserialize(data, type);
      result.add(new QueryStateItem<>(key, value, etag));
    }

    return new QueryStateResponse<>(result, token).setMetadata(metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfiguration(GetConfigurationRequest request) {
    return DaprException.wrapMono(new UnsupportedOperationException());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Flux<List<ConfigurationItem>> subscribeToConfiguration(SubscribeConfigurationRequest request) {
    return DaprException.wrapFlux(new UnsupportedOperationException());
  }

  /**
   * Converts metadata map into Query params.
   *
   * @param metadata metadata map
   * @return Query params
   */
  private static Map<String, List<String>> metadataToQueryArgs(Map<String, String> metadata) {
    if (metadata == null) {
      return Collections.emptyMap();
    }

    return metadata
        .entrySet()
        .stream()
        .filter(e -> e.getKey() != null)
        .collect(Collectors.toMap(e -> METADATA_PREFIX + e.getKey(), e -> Collections.singletonList(e.getValue())));
  }
}
