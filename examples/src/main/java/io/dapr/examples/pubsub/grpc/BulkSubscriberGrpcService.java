/*
 * Copyright 2023 The Dapr Authors
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

import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkRequestEntry;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkResponse;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkResponseEntry;
import io.dapr.v1.DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus;

/**
 * Class that encapsulates all client-side logic for Grpc.
 */
public class BulkSubscriberGrpcService extends AppCallbackGrpc.AppCallbackImplBase {

  @Override
  public void onBulkTopicEvent(io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkRequest request,
          io.grpc.stub.StreamObserver<io.dapr.v1.DaprAppCallbackProtos.TopicEventBulkResponse> responseObserver) {
    try {
      TopicEventBulkResponse.Builder responseBuilder = TopicEventBulkResponse.newBuilder();
      
      if (request.getEntriesCount() == 0) {
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
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

}
