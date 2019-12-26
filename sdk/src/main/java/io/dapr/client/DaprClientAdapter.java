package io.dapr.client;

import reactor.core.publisher.Mono;

public interface DaprClientAdapter {

  <T> Mono<Void> publishEvent(T event);

  <T, K> Mono<T> invokeService(K request, Class<T> clazz);

  <T> Mono<Void> invokeBinding(T request);

  <T, K> Mono<T> getState(K key, Class<T> clazz);

  <T> Mono<Void> saveState(T state);

  <T> Mono<Void> deleteState(T key);
}
