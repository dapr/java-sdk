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
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.query.Query;
import io.dapr.utils.TypeRef;
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
}
