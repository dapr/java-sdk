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

package io.dapr.examples.pubsub.grpc;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishRequestEntry;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkPublishResponseEntry;
import io.dapr.client.domain.CloudEvent;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Message publisher.
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run the program:
 * dapr run --components-path ./components/pubsub --app-id publisher -- \
 * java -Ddapr.grpc.port="50010" \
 * -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.CloudEventBulkPublisher
 */
public class CloudEventBulkPublisher {

  private static final int NUM_MESSAGES = 10;

  private static final String TOPIC_NAME = "testingtopic";

  //The name of the pubsub
  private static final String PUBSUB_NAME = "kafka-pubsub";

  /**
   * main method.
   * @param args incoming args
   * @throws Exception any exception
   */
  public static void main(String[] args) throws Exception {
    try (DaprPreviewClient client = (new DaprClientBuilder()).withObjectSerializer(new CustomSerializer()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      BulkPublishRequest<CloudEvent<Map<String, String>>> request = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME);
      List<BulkPublishRequestEntry<CloudEvent<Map<String, String>>>> entries = new ArrayList<>();
      for (int i = 0; i < 1; i++) {
        CloudEvent<Map<String, String>> cloudEvent = new CloudEvent<>();
        cloudEvent.setId(UUID.randomUUID().toString());
        cloudEvent.setType("example");
        cloudEvent.setSpecversion("1");
        cloudEvent.setDatacontenttype("application/json");
        String val = String.format("This is message #%d", i);
        cloudEvent.setData(new HashMap<>(){{
          put("dataKey", val);
        }});
        BulkPublishRequestEntry<CloudEvent<Map<String, String>>> entry = new BulkPublishRequestEntry<>("" + (i + 1),

            cloudEvent, CloudEvent.CONTENT_TYPE, new HashMap<>());
        entries.add(entry);
      }
      request.setEntries(entries);
      BulkPublishResponse res = client.publishEvents(request).block();
      System.out.println(res);
      if (res != null) {
        for (BulkPublishResponseEntry entry : res.getStatuses()) {
          System.out.println("EntryID : " + entry.getEntryID() + " Status : " + entry.getStatus());
        }
      } else {
        throw new Exception("null response");
      }
      System.out.println("Done");
    }
  }
}

class CustomSerializer implements DaprObjectSerializer {

  private final DefaultObjectSerializer serializer = new DefaultObjectSerializer();

  @Override
  public byte[] serialize(Object o) throws IOException {
    return serializer.serialize(o);
  }

  @Override
  public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
    return serializer.deserialize(data, type);
  }

  @Override
  public String getContentType() {
    return "application/json";
  }
}
