package io.dapr.client;

import io.dapr.exceptions.DaprException;
import io.dapr.utils.Constants;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.util.HashMap;
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
  public <T> Mono<Void> publishEvent(ClientRequest<T> event) {
    try {
      if (event.getTopic() == null || event.getTopic().trim().isEmpty()) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      if (!Constants.defaultHttpMethodSupported.PUT.name().equals(event.getHttpMethod()) &&
          !Constants.defaultHttpMethodSupported.POST.name().equals(event.getHttpMethod())) {
        throw new DaprException("405", "HTTP Method not allowed.");
      }

      String serializedEvent = objectSerializer.serialize(event.getBody());
      Map<String, String> jsonMap = new HashMap<>();
      String key = "data";
      if (event.getTopic() != null && !"".equals(event.getTopic().trim())) {
        key = event.getTopic();
      }
      jsonMap.put(key, serializedEvent);
      CompletableFuture<Void> futureVoid =
          client.publishEvent(event.getHttpMethod(), event.getTopic(), objectSerializer.serialize(jsonMap));
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
  public <T, K> Mono<T> invokeService(ClientRequest<K> request, Class<T> clazz) {
    try {
      Constants.defaultHttpMethodSupported method = Constants.defaultHttpMethodSupported.valueOf(request.getHttpMethod());
      if (method == null) {
        throw new DaprException("405", "HTTP Method not allowed.");
      }
      String serializedRequestBody = objectSerializer.serialize(request.getBody());
      Map<String, String> jsonMap = new HashMap<>();
      String key = "data";
      if (request.getTopic() != null && !"".equals(request.getTopic().trim())) {
        key = request.getTopic();
      }
      jsonMap.put(key, serializedRequestBody);
      CompletableFuture<String> futureResponse =
          client.invokeAPI(request.getHttpMethod(), request.getHttpUrl(), objectSerializer.serialize(jsonMap));
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
  public <T> Mono<Void> invokeService(ClientRequest<T> request) {
    try {
      Constants.defaultHttpMethodSupported method =
          Constants.defaultHttpMethodSupported.valueOf(request.getHttpMethod());
      if (method == null) {
        throw new DaprException("405", "HTTP Method not allowed.");
      }
      String serializedRequestBody = objectSerializer.serialize(request.getBody());
      Map<String, String> jsonMap = new HashMap<>();
      String key = "data";
      if (request.getTopic() != null && !"".equals(request.getTopic().trim())) {
        key = request.getTopic();
      }
      jsonMap.put(key, serializedRequestBody);
      CompletableFuture<Void> futureVoid =
          client.invokeAPIVoid(request.getHttpMethod(), request.getHttpUrl(), objectSerializer.serialize(jsonMap));
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
  public <T> Mono<Void> invokeBinding(ClientRequest<T> request) {
    try {
      if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }

      if (!Constants.defaultHttpMethodSupported.PUT.name().equals(request.getHttpMethod()) &&
          !Constants.defaultHttpMethodSupported.POST.name().equals(request.getHttpMethod())) {
        throw new DaprException("405", "Method not allowed.");
      }

      String serializedBidingRequestBody = objectSerializer.serialize(request.getBody());
      Map<String, String> jsonMap = new HashMap<>();
      String key = "data";
      if (request.getTopic() != null && !"".equals(request.getTopic().trim())) {
        key = request.getTopic();
      }
      jsonMap.put(key, serializedBidingRequestBody);
      CompletableFuture<Void> futureVoid =
          client.invokeBinding(request.getHttpMethod(), request.getTopic(), objectSerializer.serialize(jsonMap));
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
  public <T, K> Mono<T> getState(ClientRequest<K> key, Class<T> clazz) {
    try {
      if (key.getBody() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      String serializedKeyBody = objectSerializer.serialize(key.getBody());
      CompletableFuture<String> futureResponse = client.getState(serializedKeyBody);
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
  public <T> Mono<Void> saveState(ClientRequest<T> state) {
    try {
      if (state.getBody() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      String serializedStateBody = objectSerializer.serialize(state.getBody());
      Map<String, String> jsonMap = new HashMap<>();
      String key = "data";
      if (state.getTopic() != null && !"".equals(state.getTopic().trim())) {
        key = state.getTopic();
      }
      jsonMap.put(key, serializedStateBody);
      CompletableFuture<Void> futureVoid = client.saveState(objectSerializer.serialize(jsonMap));
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
  public <T> Mono<Void> deleteState(ClientRequest<T> key) {
    try {
      if (key.getBody() == null) {
        throw new DaprException("500", "Name cannot be null or empty.");
      }
      String serializedKey = objectSerializer.serialize(key.getBody());
      CompletableFuture<Void> futureVoid = client.deleteState(serializedKey);
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
