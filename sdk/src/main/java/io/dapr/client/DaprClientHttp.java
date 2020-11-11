/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.GetStatesRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeServiceRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.Response;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.TransactionalStateRequest;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.opentelemetry.context.Context;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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
   * Serializer for internal objects.
   */
  private static final ObjectSerializer INTERNAL_SERIALIZER = new ObjectSerializer();

  /**
   * Base path to invoke methods.
   */
  public static final String INVOKE_PATH = DaprHttp.API_VERSION + "/invoke";

  /**
   * Invoke Publish Path.
   */
  public static final String PUBLISH_PATH = DaprHttp.API_VERSION + "/publish";

  /**
   * Invoke Binding Path.
   */
  public static final String BINDING_PATH = DaprHttp.API_VERSION + "/bindings";

  /**
   * State Path.
   */
  public static final String STATE_PATH = DaprHttp.API_VERSION + "/state";

  /**
   * String format for transaction API.
   */
  private static final String TRANSACTION_URL_FORMAT = STATE_PATH + "/%s/transaction";

  /**
   * Secrets Path.
   */
  public static final String SECRETS_PATH = DaprHttp.API_VERSION + "/secrets";

  /**
   * State Path format for bulk state API.
   */
  public static final String STATE_BULK_PATH_FORMAT = STATE_PATH + "/%s/bulk";

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
  public Mono<Response<Void>> publishEvent(PublishEventRequest request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      Object data = request.getData();
      Map<String, String> metadata = request.getMetadata();
      Context context = request.getContext();

      if (topic == null || topic.trim().isEmpty()) {
        throw new IllegalArgumentException("Topic name cannot be null or empty.");
      }

      StringBuilder url = new StringBuilder(PUBLISH_PATH)
              .append("/").append(pubsubName)
              .append("/").append(topic);
      byte[] serializedEvent = objectSerializer.serialize(data);
      return this.client.invokeApi(
          DaprHttp.HttpMethods.POST.name(), url.toString(), null, serializedEvent, metadata, context)
          .thenReturn(new Response<>(context, null));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  public <T> Mono<Response<T>> invokeService(InvokeServiceRequest invokeServiceRequest, TypeRef<T> type) {
    try {
      final String appId = invokeServiceRequest.getAppId();
      final String method = invokeServiceRequest.getMethod();
      final Object request = invokeServiceRequest.getBody();
      final Map<String, String> metadata = invokeServiceRequest.getMetadata();
      final HttpExtension httpExtension = invokeServiceRequest.getHttpExtension();
      final Context context = invokeServiceRequest.getContext();
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
      String path = String.format("%s/%s/method/%s", INVOKE_PATH, appId, method);
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      Mono<DaprHttp.Response> response = this.client.invokeApi(httMethod, path,
          httpExtension.getQueryString(), serializedRequestBody, metadata, context);
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
      }).map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<T>> invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
    try {
      final String name = request.getName();
      final String operation = request.getOperation();
      final Object data = request.getData();
      final Map<String, String> metadata = request.getMetadata();
      final Context context = request.getContext();
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

      StringBuilder url = new StringBuilder(BINDING_PATH).append("/").append(name);

      byte[] payload = INTERNAL_SERIALIZER.serialize(jsonMap);
      String httpMethod = DaprHttp.HttpMethods.POST.name();
      Mono<DaprHttp.Response> response = this.client.invokeApi(
              httpMethod, url.toString(), null, payload, null, context);
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
      }).map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<List<State<T>>>> getStates(GetStatesRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<String> keys = request.getKeys();
      final int parallelism = request.getParallelism();
      final Context context = request.getContext();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (keys == null || keys.isEmpty()) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      if (parallelism < 0) {
        throw new IllegalArgumentException("Parallelism cannot be negative.");
      }

      String url = String.format(STATE_BULK_PATH_FORMAT, stateStoreName);
      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("keys", keys);
      jsonMap.put("parallelism", parallelism);

      byte[] requestBody = INTERNAL_SERIALIZER.serialize(jsonMap);
      return this.client
          .invokeApi(DaprHttp.HttpMethods.POST.name(), url, null, requestBody, null, context)
          .flatMap(s -> {
            try {
              return Mono.just(buildStates(s, type));
            } catch (Exception ex) {
              return Mono.error(ex);
            }
          })
          .map(r -> new Response<>(context, r));

    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }
  

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Response<State<T>>> getState(GetStateRequest request, TypeRef<T> type) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Context context = request.getContext();

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

      StringBuilder url = new StringBuilder(STATE_PATH)
          .append("/")
          .append(stateStoreName)
          .append("/")
          .append(key);
      Map<String, String> urlParameters = Optional.ofNullable(options)
          .map(o -> o.getStateOptionsAsMap())
          .orElse(new HashMap<>());

      request.getMetadata().forEach(urlParameters::put);

      return this.client
          .invokeApi(DaprHttp.HttpMethods.GET.name(), url.toString(), urlParameters, headers, context)
          .flatMap(s -> {
            try {
              return Mono.just(buildState(s, key, options, type));
            } catch (Exception ex) {
              return Mono.error(ex);
            }
          })
          .map(r -> new Response<>(context, r));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> executeTransaction(ExecuteStateTransactionRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<TransactionalStateOperation<?>> operations = request.getOperations();
      final Map<String, String> metadata = request.getMetadata();
      final Context context = request.getContext();
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (operations == null || operations.isEmpty()) {
        return Mono.empty();
      }
      final Map<String, String> headers = new HashMap<>();
      final String etag = operations.stream()
          .filter(op -> null != op.getRequest().getEtag() && !op.getRequest().getEtag().trim().isEmpty())
          .findFirst()
          .orElse(new TransactionalStateOperation<>(null, new State<>(null,null, null, null)))
          .getRequest()
          .getEtag();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(HEADER_HTTP_ETAG_ID, etag);
      }
      final String url = String.format(TRANSACTION_URL_FORMAT,  stateStoreName);
      List<TransactionalStateOperation<Object>> internalOperationObjects = new ArrayList<>(operations.size());
      for (TransactionalStateOperation operation : operations) {
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
        operations.add(new TransactionalStateOperation<>(operation.getOperation(),
            new State<>(data, state.getKey(), state.getEtag(), state.getOptions())));
      }
      TransactionalStateRequest<Object> req = new TransactionalStateRequest<>(internalOperationObjects, metadata);
      byte[] serializedOperationBody = INTERNAL_SERIALIZER.serialize(req);
      return this.client.invokeApi(
          DaprHttp.HttpMethods.POST.name(), url, null, serializedOperationBody, headers, context)
          .thenReturn(new Response<>(context, null));
    } catch (IOException e) {
      return Mono.error(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> saveStates(SaveStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final List<State<?>> states = request.getStates();
      final Context context = request.getContext();
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
        headers.put(HEADER_HTTP_ETAG_ID, etag);
      }
      final String url = STATE_PATH + "/" + stateStoreName;
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
          DaprHttp.HttpMethods.POST.name(), url, null, serializedStateBody, headers, context)
          .thenReturn(new Response<>(context, null));
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Void>> deleteState(DeleteStateRequest request) {
    try {
      final String stateStoreName = request.getStateStoreName();
      final String key = request.getKey();
      final StateOptions options = request.getStateOptions();
      final String etag = request.getEtag();
      final Context context = request.getContext();

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
      String url = STATE_PATH + "/" + stateStoreName + "/" + key;
      Map<String, String> urlParameters = Optional.ofNullable(options)
          .map(stateOptions -> stateOptions.getStateOptionsAsMap())
          .orElse(new HashMap<>());

      request.getMetadata().forEach(urlParameters::put);

      return this.client.invokeApi(
              DaprHttp.HttpMethods.DELETE.name(), url, urlParameters, headers, context)
          .thenReturn(new Response<>(context, null));
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
    return new State<>(value, requestedKey, etag, stateOptions);
  }

  /**
   * Builds a State object based on the Response.
   *
   * @param response     The response of the HTTP Call
   * @param type        The Class of the Value of the state
   * @param <T>          The Type of the Value of the state
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
      // TODO(artursouza): JSON cannot differentiate if data returned is String or byte[], it is ambiguous.
      // This is not a high priority since GRPC is the default (and recommended) client implementation.
      byte[] data = node.path("data").toString().getBytes(Properties.STRING_CHARSET.get());
      T value = stateSerializer.deserialize(data, type);
      result.add(new State<>(value, key, etag));
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Response<Map<String, String>>> getSecret(GetSecretRequest request) {
    String secretStoreName = request.getSecretStoreName();
    String key = request.getKey();
    Map<String, String> metadata = request.getMetadata();
    Context context = request.getContext();
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret key cannot be null or empty.");
      }
    } catch (Exception e) {
      return Mono.error(e);
    }

    String url = SECRETS_PATH + "/" + secretStoreName + "/" + key;
    return this.client
      .invokeApi(DaprHttp.HttpMethods.GET.name(), url, metadata, (String)null, null, context)
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
      .map(m -> (Map<String, String>)m)
      .map(m -> new Response<>(context, m));
  }

  @Override
  public void close() throws IOException {
    client.close();
  }
}
