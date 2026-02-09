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

package io.dapr.examples.pubsub.grpc;

import com.google.protobuf.Empty;
import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkRequestEntry;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkResponse;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkResponseEntry;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that encapsulates all client-side logic for Grpc.
 */
public class SubscriberGrpcService extends AppCallbackGrpc.AppCallbackImplBase {
  private final List<DaprAppCallbackProtos.TopicSubscription> topicSubscriptionList = new ArrayList<>();
  
  public static final Context.Key<Metadata> METADATA_KEY = Context.key("grpc-metadata");
  // gRPC interceptor to capture metadata
  public static class MetadataInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context contextWithMetadata = Context.current().withValue(METADATA_KEY, headers);
      return Contexts.interceptCall(contextWithMetadata, call, headers, next);
    }
  }
  
  @Override
  public void listTopicSubscriptions(Empty request,
      StreamObserver<DaprAppCallbackProtos.ListTopicSubscriptionsResponse> responseObserver) {
    registerConsumer("messagebus", "testingtopic", false);
    registerConsumer("messagebus", "bulkpublishtesting", false);
    registerConsumer("messagebus", "testingtopicbulk", true);
    try {
      DaprAppCallbackProtos.ListTopicSubscriptionsResponse.Builder builder = DaprAppCallbackProtos
          .ListTopicSubscriptionsResponse.newBuilder();
      topicSubscriptionList.forEach(builder::addSubscriptions);
      DaprAppCallbackProtos.ListTopicSubscriptionsResponse response = builder.build();
      responseObserver.onNext(response);
    } catch (Throwable e) {
      responseObserver.onError(e);
    } finally {
      responseObserver.onCompleted();
    }
  }

  @Override
  public void onTopicEvent(DaprAppCallbackProtos.TopicEventRequest request,
      StreamObserver<DaprAppCallbackProtos.TopicEventResponse> responseObserver) {
    try {
      try {
        Context context = Context.current();
        Metadata metadata = METADATA_KEY.get(context);
        
        if (metadata != null) {
          System.out.println("Metadata found in context");
          String apiToken = metadata.get(Metadata.Key.of("dapr-api-token", Metadata.ASCII_STRING_MARSHALLER));
          if (apiToken != null) {
            System.out.println("API Token extracted: " + apiToken);
          } else {
            System.out.println("No 'dapr-api-token' found in metadata");
          }
          System.out.println("All metadata:");
          for (String key : metadata.keys()) {
            String value = metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            System.out.println("key: " + key + ": " + value);
          }
        } else {
          System.out.println("No metadata found in context");
        }
      } catch (Exception e) {
        System.out.println(" Error extracting metadata: " + e.getMessage());
      }
      
      String data = request.getData().toStringUtf8().replace("\"", "");
      System.out.println("Subscriber got: " + data);
      DaprAppCallbackProtos.TopicEventResponse response = DaprAppCallbackProtos.TopicEventResponse.newBuilder()
          .setStatus(DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.SUCCESS)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Throwable e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void onBulkTopicEvent(DaprAppCallbackProtos.TopicEventBulkRequest request,
      StreamObserver<TopicEventBulkResponse> responseObserver) {
    try {
      TopicEventBulkResponse.Builder responseBuilder = TopicEventBulkResponse.newBuilder();
      
      if (request.getEntriesCount() == 0) {
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
        return;
      }

      System.out.println("Bulk Subscriber received " + request.getEntriesCount() + " messages.");

      for (TopicEventBulkRequestEntry entry : request.getEntriesList()) {
        try {
          System.out.printf("Bulk Subscriber message has entry ID: %s\n", entry.getEntryId());
          System.out.printf("Bulk Subscriber got: %s\n", entry.getCloudEvent().getData().toStringUtf8());
          TopicEventBulkResponseEntry.Builder responseEntryBuilder = TopicEventBulkResponseEntry
              .newBuilder()
              .setEntryId(entry.getEntryId())
              .setStatusValue(TopicEventResponseStatus.SUCCESS_VALUE);
          responseBuilder.addStatuses(responseEntryBuilder);
        } catch (Throwable e) {
          TopicEventBulkResponseEntry.Builder responseEntryBuilder = TopicEventBulkResponseEntry
              .newBuilder()
              .setEntryId(entry.getEntryId())
              .setStatusValue(TopicEventResponseStatus.RETRY_VALUE);
          responseBuilder.addStatuses(responseEntryBuilder);
        }
      }
      TopicEventBulkResponse response = responseBuilder.build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Throwable e) {
      responseObserver.onError(e);
    }
  }

  /**
   * Add pubsub name and topic to topicSubscriptionList.
   * 
   * @param topic the topic
   * @param pubsubName the pubsub name
   * @param isBulkMessage flag to enable/disable bulk subscribe
   */
  public void registerConsumer(String pubsubName, String topic, boolean isBulkMessage) {
    topicSubscriptionList.add(DaprAppCallbackProtos.TopicSubscription
        .newBuilder()
        .setPubsubName(pubsubName)
        .setTopic(topic)
        .setBulkSubscribe(DaprAppCallbackProtos.BulkSubscribeConfig.newBuilder().setEnabled(isBulkMessage))
        .build());
  }
}

