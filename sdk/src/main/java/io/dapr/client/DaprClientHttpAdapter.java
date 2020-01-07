package io.dapr.client;

import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

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
  public <T, K> Mono<T> getState(StateKeyValue<K> state, StateOptions stateOptions, Class<T> clazz) {
    try {
      if (state.getKey() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (state.getEtag() != null && !state.getEtag().trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, state.getEtag());
      }
      String serializedKeyBody = objectSerializer.serialize(state.getKey());
      if (stateOptions.getConsistency() != null && !stateOptions.getConsistency().trim().isEmpty()) {
        serializedKeyBody += "?consistency=" + objectSerializer.serialize(stateOptions.getConsistency());
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
  public <T> Mono<Void> saveStates(List<StateKeyValue<T>> states) {
    try {
      if (states == null || states.isEmpty()) {
        return Mono.empty();
      }
      Map<String, String> headers = new HashMap<>();
      String etag = states.stream().filter(state -> null != state.getEtag() && !state.getEtag().trim().isEmpty())
          .findFirst().orElse(new StateKeyValue<>()).getEtag();
      if (etag != null && !etag.trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, etag);
      }
      String url = Constants.STATE_PATH;
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
  public <T> Mono<Void> saveState(String key, String etag, T value) {
    StateKeyValue<T> state = new StateKeyValue<>(value, key, etag);
    return saveStates(Arrays.asList(state));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> deleteState(StateKeyValue<T> state, StateOptions stateOptions) {
    try {
      if (state.getKey() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      Map<String, String> headers = new HashMap<>();
      if (state.getEtag() != null && !state.getEtag().trim().isEmpty()) {
        headers.put(Constants.HEADER_HTTP_ETAG_ID, state.getEtag());
      }
      String serializedKey = objectSerializer.serialize(state.getKey());
      if (stateOptions.getConsistency() != null && !stateOptions.getConsistency().trim().isEmpty()) {
        serializedKey += "?consistency=" + objectSerializer.serialize(stateOptions.getConsistency());
      }
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
  public Mono<Void> saveStateTransactionally(String actorType, String actorId, String data) {
    String url = String.format(Constants.ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return actorActionVoid(Constants.defaultHttpMethodSupported.PUT.name(), url, data);
  }

  @Override
  public Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.PUT.name(), url, data);
  }

  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(Constants.ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.DELETE.name(), url, null);
  }

  @Override
  public Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data) {
    String url = String.format(Constants.ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return actorActionVoid(Constants.defaultHttpMethodSupported.PUT.name(), url, data);
  }

  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
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
}
