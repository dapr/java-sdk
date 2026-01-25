/*
 * Copyright 2022 The Dapr Authors
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

import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkPublishResponseFailedEntry;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponse;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.DecryptRequestAlpha1;
import io.dapr.client.domain.EncryptRequestAlpha1;
import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.query.Query;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Generic client interface for preview or alpha APIs in Dapr, regardless of GRPC or HTTP.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprPreviewClient extends AutoCloseable {

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param metadata Optional metadata passed to the state store.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query,
                                             Map<String, String> metadata, Class<T> clazz);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param metadata Optional metadata passed to the state store.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query,
                                             Map<String, String> metadata, TypeRef<T> type);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, Class<T> clazz);

  /**
   * Query for states using a query string.
   *
   * @param storeName Name of the state store to query.
   * @param query String value of the query.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, TypeRef<T> type);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param metadata Optional metadata passed to the state store.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query,
                                             Map<String, String> metadata, Class<T> clazz);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param metadata Optional metadata passed to the state store.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query,
                                             Map<String, String> metadata, TypeRef<T> type);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, Class<T> clazz);

  /**
   * Query for states using a query domain object.
   *
   * @param storeName Name of the state store to query.
   * @param query Query value domain object.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, TypeRef<T> type);

  /**
   * Query for states using a query request.
   *
   * @param request Query request object.
   * @param clazz The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, Class<T> clazz);

  /**
   * Query for states using a query request.
   *
   * @param request Query request object.
   * @param type The type needed as return for the call.
   * @param <T> The Type of the return, use byte[] to skip serialization.
   * @return A Mono of QueryStateResponse of type T.
   */
  <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, TypeRef<T> type);

  /**
   * Publish multiple events to Dapr in a single request.
   *
   * @param request {@link BulkPublishRequest} object.
   * @return A Mono of {@link BulkPublishResponse} object.
   * @param <T> The type of events to publish in the call.
   */
  <T> Mono<BulkPublishResponse<T>> publishEvents(BulkPublishRequest<T> request);

  /**
   * Publish multiple events to Dapr in a single request.
   *
   * @param pubsubName the pubsub name we will publish the event to.
   * @param topicName the topicName where the event will be published.
   * @param events the {@link List} of events to be published.
   * @param contentType the content type of the event. Use Mime based types.
   * @return the {@link BulkPublishResponse} containing publish status of each event.
   *     The "entryID" field in {@link BulkPublishEntry} in {@link BulkPublishResponseFailedEntry} will be
   *     generated based on the order of events in the {@link List}.
   * @param <T> The type of the events to publish in the call.
   */
  <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName, String contentType,
                                                 List<T> events);

  /**
   * Publish multiple events to Dapr in a single request.
   *
   * @param pubsubName the pubsub name we will publish the event to.
   * @param topicName the topicName where the event will be published.
   * @param events the varargs of events to be published.
   * @param contentType the content type of the event. Use Mime based types.
   * @return the {@link BulkPublishResponse} containing publish status of each event.
   *     The "entryID" field in {@link BulkPublishEntry} in {@link BulkPublishResponseFailedEntry} will be
   *     generated based on the order of events in the {@link List}.
   * @param <T> The type of the events to publish in the call.
   */
  <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName, String contentType,
                                                 T... events);

  /**
   * Publish multiple events to Dapr in a single request.
   *
   * @param pubsubName the pubsub name we will publish the event to.
   * @param topicName the topicName where the event will be published.
   * @param events the {@link List} of events to be published.
   * @param contentType the content type of the event. Use Mime based types.
   * @param requestMetadata the metadata to be set at the request level for the {@link BulkPublishRequest}.
   * @return the {@link BulkPublishResponse} containing publish status of each event.
   *     The "entryID" field in {@link BulkPublishEntry} in {@link BulkPublishResponseFailedEntry} will be
   *     generated based on the order of events in the {@link List}.
   * @param <T> The type of the events to publish in the call.
   */
  <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName, String contentType,
                                                 Map<String,String> requestMetadata, List<T> events);

  /**
   * Publish multiple events to Dapr in a single request.
   *
   * @param pubsubName the pubsub name we will publish the event to.
   * @param topicName the topicName where the event will be published.
   * @param events the varargs of events to be published.
   * @param contentType the content type of the event. Use Mime based types.
   * @param requestMetadata the metadata to be set at the request level for the {@link BulkPublishRequest}.
   * @return the {@link BulkPublishResponse} containing publish status of each event.
   *     The "entryID" field in {@link BulkPublishEntry} in {@link BulkPublishResponseFailedEntry} will be
   *     generated based on the order of events in the {@link List}.
   * @param <T> The type of the events to publish in the call.
   */
  <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName, String contentType,
                                                 Map<String,String> requestMetadata, T... events);


  /**
   * Tries to get a lock with an expiry.
   * @param storeName Name of the store
   * @param resourceId Lock key
   * @param lockOwner The identifier of lock owner
   * @param expiryInSeconds The time before expiry
   * @return Whether the lock is successful
   */
  Mono<Boolean> tryLock(String storeName, String resourceId, String lockOwner, Integer expiryInSeconds);

  /**
   * Tries to get a lock with an expiry.
   * @param request The request to lock
   * @return Whether the lock is successful
   */
  Mono<Boolean> tryLock(LockRequest request);

  /**
   * Unlocks a lock.
   * @param storeName Name of the store
   * @param resourceId Lock key
   * @param lockOwner The identifier of lock owner
   * @return Unlock result
   */
  Mono<UnlockResponseStatus> unlock(String storeName, String resourceId, String lockOwner);

  /**
   * Unlocks a lock.
   * @param request The request to unlock
   * @return Unlock result
   */
  Mono<UnlockResponseStatus> unlock(UnlockRequest request);

  /**
   * Subscribe to pubsub via streaming.
   * @param pubsubName Name of the pubsub component.
   * @param topic Name of the topic to subscribe to.
   * @param listener Callback methods to process events.
   * @param type Type for object deserialization.
   * @param <T> Type of object deserialization.
   * @return An active subscription.
   * @deprecated Use {@link #subscribeToTopic(String, String, TypeRef)} instead for a more reactive approach.
   */
  @Deprecated
  <T> Subscription subscribeToEvents(
      String pubsubName, String topic, SubscriptionListener<T> listener, TypeRef<T> type);

  /**
   * Subscribe to pubsub events via streaming using Project Reactor Flux.
   *
   * <p>The type parameter determines what is deserialized from the event data:
   * <ul>
   *   <li>Use {@code TypeRef.STRING} or similar for raw payload data</li>
   *   <li>Use {@code new TypeRef<CloudEvent<String>>(){}} to receive CloudEvent with metadata</li>
   * </ul>
   *
   * @param pubsubName Name of the pubsub component.
   * @param topic Name of the topic to subscribe to.
   * @param type Type for object deserialization.
   * @return A Flux of deserialized event payloads.
   * @param <T> Type of the event payload.
   */
  <T> Flux<T> subscribeToTopic(String pubsubName, String topic, TypeRef<T> type);

  /**
   * Subscribe to pubsub events via streaming using Project Reactor Flux with metadata support.
   *
   * <p>If metadata is null or empty, this method delegates to {@link #subscribeToTopic(String, String, TypeRef)}.
   * Use metadata {@code {"rawPayload": "true"}} for raw payload subscriptions where Dapr
   * delivers messages without CloudEvent wrapping.
   *
   * @param pubsubName Name of the pubsub component.
   * @param topic Name of the topic to subscribe to.
   * @param type Type for object deserialization.
   * @param metadata Subscription metadata (e.g., {"rawPayload": "true"}).
   * @return A Flux of deserialized event payloads.
   * @param <T> Type of the event payload.
   */
  <T> Flux<T> subscribeToTopic(String pubsubName, String topic, TypeRef<T> type, Map<String, String> metadata);

  /*
   * Converse with an LLM.
   *
   * @param conversationRequest request to be passed to the LLM.
   * @return {@link ConversationResponse}.
   */
  @Deprecated
  public Mono<ConversationResponse> converse(ConversationRequest conversationRequest);

  /*
   * Converse with an LLM using Alpha2 API.
   *
   * @param conversationRequestAlpha2 request to be passed to the LLM with Alpha2 features.
   * @return {@link ConversationResponseAlpha2}.
   */
  public Mono<ConversationResponseAlpha2> converseAlpha2(ConversationRequestAlpha2 conversationRequestAlpha2);

  /**
   * Encrypt data using the Dapr cryptography building block.
   * This method uses streaming to handle large payloads efficiently.
   *
   * @param request The encryption request containing component name, key information, and plaintext stream.
   * @return A Flux of encrypted byte arrays (ciphertext chunks).
   * @throws IllegalArgumentException if required parameters are missing.
   */
  Flux<byte[]> encrypt(EncryptRequestAlpha1 request);

  /**
   * Decrypt data using the Dapr cryptography building block.
   * This method uses streaming to handle large payloads efficiently.
   *
   * @param request The decryption request containing component name, optional key name, and ciphertext stream.
   * @return A Flux of decrypted byte arrays (plaintext chunks).
   * @throws IllegalArgumentException if required parameters are missing.
   */
  Flux<byte[]> decrypt(DecryptRequestAlpha1 request);
}
