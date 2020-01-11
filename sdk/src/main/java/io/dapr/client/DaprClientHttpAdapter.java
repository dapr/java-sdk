/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An adapter for the HTTP Client.
 *
 * @see io.dapr.client.DaprHttp
 * @see io.dapr.client.DaprClient
 */
public class DaprClientHttpAdapter implements DaprClient {

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
        throw new DaprException("INVALID_TOPIC", "Topic name cannot be null or empty.");
      }

      byte[] serializedEvent = objectSerializer.serialize(event);
      StringBuilder url = new StringBuilder(Constants.PUBLISH_PATH).append("/").append(topic);
      return this.client.invokeAPI(
          DaprHttp.HttpMethods.POST.name(), url.toString(), serializedEvent, metadata).then();
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
        throw new DaprException("500", "Verb cannot be null.");
      }
      String httMethod = verb.toString();
      if (appId == null || appId.trim().isEmpty()) {
        throw new DaprException("500", "App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new DaprException("500", "Method name cannot be null or empty.");
      }
      String path = String.format("%s/%s/method/%s", Constants.INVOKE_PATH, appId, method);
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      return this.client.invokeAPI(httMethod, path, serializedRequestBody, metadata)
          .flatMap(r -> {
            try {
              return Mono.just(objectSerializer.deserialize(r, clazz));
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
        throw new DaprException("500", "Name to bind cannot be null or empty.");
      }

      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("data", request);
      StringBuilder url = new StringBuilder(Constants.BINDING_PATH).append("/").append(name);

      return this.client
          .invokeAPI(
              DaprHttp.HttpMethods.POST.name(),
              url.toString(),
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
  public <T, K> Mono<T> getState(StateKeyValue<K> state, StateOptions options, Class<T> clazz) {
    try {
      if (state.getKey() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (state.getEtag() != null && !state.getEtag().trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, state.getEtag());
      }

      StringBuilder url = new StringBuilder(Constants.STATE_PATH)
        .append("/")
        .append(state.getKey())
        .append(getOptionsAsQueryParameter(options));
      return this.client
          .invokeAPI(DaprHttp.HttpMethods.GET.name(), url.toString(), headers)
          .flatMap(s -> {
            try {
              return Mono.just(objectSerializer.deserialize(s, clazz));
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
  public <T> Mono<Void> saveStates(List<StateKeyValue<T>> states, StateOptions options) {
    try {
      if (states == null || states.isEmpty()) {
        return Mono.empty();
      }
      Map<String, String> headers = new HashMap<>();
      String etag = states.stream().filter(state -> null != state.getEtag() && !state.getEtag().trim().isEmpty())
          .findFirst().orElse(new StateKeyValue<>(null, null, null)).getEtag();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }
      String url = Constants.STATE_PATH + getOptionsAsQueryParameter(options);;
      byte[] serializedStateBody = objectSerializer.serialize(states);
      return this.client.invokeAPI(
        DaprHttp.HttpMethods.POST.name(), url, serializedStateBody, headers).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> saveState(String key, String etag, T value, StateOptions options) {
    StateKeyValue<T> state = new StateKeyValue<>(value, key, etag);
    return saveStates(Arrays.asList(state), options);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> deleteState(StateKeyValue<T> state, StateOptions options) {
    try {
      if (state.getKey() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (state.getEtag() != null && !state.getEtag().trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, state.getEtag());
      }
      String url = Constants.STATE_PATH + "/" + state.getKey() + getOptionsAsQueryParameter(options);
      return this.client.invokeAPI(DaprHttp.HttpMethods.DELETE.name(), url, headers).then();
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
    String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.POST.name(), url, jsonPayload, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> getActorState(String actorType, String actorId, String keyName) {
    String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.GET.name(), url, "", null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveActorStateTransactionally(String actorType, String actorId, String data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return this.client.invokeAPI(DaprHttp.HttpMethods.PUT.name(), url, data, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerActorReminder(String actorType, String actorId, String reminderName, String data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.PUT.name(), url, data, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterActorReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.DELETE.name(), url, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerActorTimer(String actorType, String actorId, String timerName, String data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.PUT.name(), url, data, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterActorTimer(String actorType, String actorId, String timerName) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeAPI(DaprHttp.HttpMethods.DELETE.name(), url, null).then();
  }

  /**
   * Gets the string with params for a given URL.
   *
   * TODO: Move this logic down the stack to use okhttp's builder instead:
   *   https://square.github.io/okhttp/4.x/okhttp/okhttp3/-http-url/-builder/add-query-parameter/
   * @param options State options to be converted.
   * @return String with query params.
   * @throws IllegalAccessException Cannot extract params.
   */
  private String getOptionsAsQueryParameter(StateOptions options)
      throws IllegalAccessException {
    StringBuilder sb = new StringBuilder();
    Map<String, Object> mapOptions = transformStateOptionsToMap(options);
    if (mapOptions != null && !mapOptions.isEmpty()) {
      sb.append("?");
      for (Map.Entry<String, Object> option : mapOptions.entrySet()) {
        sb.append(option.getKey()).append("=").append(option.getValue()).append("&");
      }
      sb.deleteCharAt(sb.length()-1);
    }
    return sb.toString();
  }

  /**
   * Converts state options to map.
   *
   * TODO: Move this logic to StateOptions.
   * @param options Instance to have is methods converted into map.
   * @return Map for the state options.
   * @throws IllegalAccessException Cannot extract params.
   */
  private Map<String, Object> transformStateOptionsToMap(StateOptions options)
      throws IllegalAccessException {
    Map<String, Object> mapOptions = null;
    if (options != null) {
      mapOptions = new HashMap<>();
      for (Field field : options.getClass().getFields()) {
        Object fieldValue = field.get(options);
        if (fieldValue != null) {
          mapOptions.put(field.getName(), fieldValue);
        }
      }
    }
    return mapOptions;
  }

}
