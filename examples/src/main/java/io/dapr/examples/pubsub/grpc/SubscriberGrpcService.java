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
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that encapsulates all client-side logic for Grpc.
 */
public class SubscriberGrpcService extends AppCallbackGrpc.AppCallbackImplBase {
  private final List<DaprAppCallbackProtos.TopicSubscription> topicSubscriptionList = new ArrayList<>();
  
  @Override
  public void listTopicSubscriptions(Empty request,
      StreamObserver<DaprAppCallbackProtos.ListTopicSubscriptionsResponse> responseObserver) {
    registerConsumer("messagebus","testingtopic");
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

  /**
   * Add pubsub name and topic to topicSubscriptionList.
   * 
   * @param topic the topic
   * @param pubsubName the pubsub name
   */
  public void registerConsumer(String pubsubName, String topic) {
    topicSubscriptionList.add(DaprAppCallbackProtos.TopicSubscription
        .newBuilder()
        .setPubsubName(pubsubName)
        .setTopic(topic)
        .build());
  }
}

