package io.dapr.client;

import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An adapter for the GRPC Client.
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
  private DaprHttp client;

  /**
   * A utitlity class for serialize and deserialize the messages sent and retrived by the client.
   */
  private ObjectSerializer objectSerializer;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param httpClient
   * @see io.dapr.client.DaprClientBuilder
   */
  DaprClientHttpAdapter(DaprHttp httpClient) {
    this.client = client;
    objectSerializer = new ObjectSerializer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> publishEvent(String topic, T event) {
    try {
      if (topic == null || topic.trim().isEmpty()) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      String serializedEvent = objectSerializer.serialize(event);
      StringBuilder url = new StringBuilder(Constants.PUBLISH_PATH).append("/").append(topic);
      CompletableFuture<Void> futureVoid = client.invokeAPIVoid(
          Constants.defaultHttpMethodSupported.POST.name(), url.toString(), serializedEvent, null);
      return Mono.just(futureVoid).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, K> Mono<T> invokeService(String verb, String appId, String method, K request, Class<T> clazz) {
    try {
      if (verb == null || verb.trim().isEmpty()) {
        throw new DaprException("500", "App Id cannot be null or empty.");
      }
      Constants.defaultHttpMethodSupported httMethod = Constants.defaultHttpMethodSupported.valueOf(verb.toUpperCase());
      if (httMethod == null) {
        throw new DaprException("405", "HTTP Method not allowed.");
      }
      if (appId == null || appId.trim().isEmpty()) {
        throw new DaprException("500", "App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new DaprException("500", "App Id cannot be null or empty.");
      }
      StringBuilder urlSB = new StringBuilder("/invoke/");
      urlSB.append(objectSerializer.serialize(appId));
      urlSB.append("/method/");
      urlSB.append(objectSerializer.serialize(method));
      String serializedRequestBody = objectSerializer.serialize(request);
      CompletableFuture<String> futureResponse =
          client.invokeAPI(httMethod.name(), urlSB.toString(), serializedRequestBody, null);
      return Mono.just(futureResponse).flatMap(f -> {
        try {
          return Mono.just(objectSerializer.deserialize(f.get(), clazz));
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
  public <T> Mono<Void> invokeService(String verb, String appId, String method, T request) {
    try {
      if (verb == null || verb.trim().isEmpty()) {
        throw new DaprException("500", "App Id cannot be null or empty.");
      }
      Constants.defaultHttpMethodSupported httMethod = Constants.defaultHttpMethodSupported.valueOf(verb.toUpperCase());
      if (httMethod == null) {
        throw new DaprException("405", "HTTP Method not allowed.");
      }
      if (appId == null || appId.trim().isEmpty()) {
        throw new DaprException("500", "App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new DaprException("500", "Method to invoke cannot be null or empty.");
      }
      StringBuilder urlSB = new StringBuilder("/invoke/");
      urlSB.append(objectSerializer.serialize(appId));
      urlSB.append("/method/");
      urlSB.append(objectSerializer.serialize(method));
      String serializedRequestBody = objectSerializer.serialize(request);
      CompletableFuture<Void> futureVoid =
          client.invokeAPIVoid(httMethod.name(), urlSB.toString(), serializedRequestBody, null);
      return Mono.just(futureVoid).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
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

      String serializedBidingRequestBody = objectSerializer.serialize(request);

      Map<String, String> jsonMap = new HashMap<>();
      jsonMap.put("Data", serializedBidingRequestBody);
      StringBuilder url = new StringBuilder(Constants.BINDING_PATH).append("/").append(name);
      CompletableFuture<Void> futureVoid = client.invokeAPIVoid(
          Constants.defaultHttpMethodSupported.POST.name(), url.toString(), objectSerializer.serialize(jsonMap), null);
      return Mono.just(futureVoid).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
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
      String serializedKeyBody = objectSerializer.serialize(state.getKey());
      serializedKeyBody += getOptionsAsQueryParameter(options);
      if (options.getConsistency() != null && !options.getConsistency().trim().isEmpty()) {
        serializedKeyBody += "?consistency=" + objectSerializer.serialize(options.getConsistency());
      }
      StringBuilder url = new StringBuilder(Constants.STATE_PATH).append("/").append(serializedKeyBody);
      CompletableFuture<String> futureResponse =
          client.invokeAPI(Constants.defaultHttpMethodSupported.GET.name(), url.toString(), null, headers);
      return Mono.just(futureResponse).flatMap(f -> {
        try {
          return Mono.just(objectSerializer.deserialize(f.get(), clazz));
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
      String serializedStateBody = objectSerializer.serialize(states);
      CompletableFuture<Void> futureVoid = client.invokeAPIVoid(
          Constants.defaultHttpMethodSupported.POST.name(), url, serializedStateBody, headers);
      return Mono.just(futureVoid).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

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
      String serializedKey = objectSerializer.serialize(state.getKey());
      serializedKey += getOptionsAsQueryParameter(options);
      String url = Constants.STATE_PATH + "/" + serializedKey;
      CompletableFuture<Void> futureVoid = client.invokeAPIVoid(
          Constants.defaultHttpMethodSupported.DELETE.name(), url, null, headers);
      return Mono.just(futureVoid).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
    String url = String.format(Constants.ACTOR_METHOD_RELATIVE_URL_FORMAT, actorType, actorId, methodName);
    return actorActionString(Constants.defaultHttpMethodSupported.POST.name(), url, jsonPayload);
  }

  @Override
  public Mono<String> getActorState(String actorType, String actorId, String keyName) {
    String url = String.format(Constants.ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    return actorActionString(Constants.defaultHttpMethodSupported.GET.name(), url, null);
  }

  @Override
  public Mono<Void> saveActorStateTransactionally(String actorType, String actorId, String data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return actorActionVoid(Constants.defaultHttpMethodSupported.PUT.name(), url, data);
  }

  @Override
  public Mono<Void> registerActorReminder(String actorType, String actorId, String reminderName, String data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.PUT.name(), url, data);
  }

  @Override
  public Mono<Void> unregisterActorReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.DELETE.name(), url, null);
  }

  @Override
  public Mono<Void> registerActorTimer(String actorType, String actorId, String timerName, String data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.PUT.name(), url, data);
  }

  @Override
  public Mono<Void> unregisterActorTimer(String actorType, String actorId, String timerName) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.DELETE.name(), url, null);
  }

  private Mono<String> actorActionString(String httpVerb, String url, String payload) {
    try {
      CompletableFuture<String> futureResponse =
          client.invokeAPI(httpVerb, url, objectSerializer.serialize(payload), null);
      return Mono.just(futureResponse).flatMap(f -> {
        try {
          return Mono.just(objectSerializer.deserialize(f.get(), String.class));
        } catch (Exception ex) {
          return Mono.error(ex);
        }
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  private Mono<Void> actorActionVoid(String httpVerb, String url, String payload) {
    try {
      CompletableFuture<Void> futureVoid =
          client.invokeAPIVoid(httpVerb, url, objectSerializer.serialize(payload), null);
      return Mono.just(futureVoid).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  private String getOptionsAsQueryParameter(StateOptions options)
      throws IllegalAccessException, IllegalArgumentException, IOException {
    StringBuilder sb = new StringBuilder();
    Map<String, Object> mapOptions = transformStateOptionsToMap(options);
    if (mapOptions != null && !mapOptions.isEmpty()) {
      sb.append("?");
      for (Map.Entry<String, Object> option : mapOptions.entrySet()) {
        sb.append(option.getKey()).append("=").append(objectSerializer.serialize(option.getValue())).append("&");
      }
      sb.deleteCharAt(sb.length()-1);
    }
    return sb.toString();
  }

  private Map<String, Object> transformStateOptionsToMap(StateOptions options)
      throws IllegalAccessException, IllegalArgumentException {
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
