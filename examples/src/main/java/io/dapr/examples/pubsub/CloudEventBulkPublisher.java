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

package io.dapr.examples.pubsub;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkPublishResponseFailedEntry;
import io.dapr.client.domain.CloudEvent;

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
 * -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.CloudEventBulkPublisher
 */
public class CloudEventBulkPublisher {

  private static final int NUM_MESSAGES = 10;

  private static final String TOPIC_NAME = "bulkpublishtesting";

  //The name of the pubsub
  private static final String PUBSUB_NAME = "messagebus";

  /**
   * main method.
   *
   * @param args incoming args
   * @throws Exception any exception
   */
  public static void main(String[] args) throws Exception {
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      List<BulkPublishEntry<CloudEvent<Map<String, String>>>> entries = new ArrayList<>();
      for (int i = 0; i < NUM_MESSAGES; i++) {
        CloudEvent<Map<String, String>> cloudEvent = new CloudEvent<>();
        cloudEvent.setId(UUID.randomUUID().toString());
        cloudEvent.setType("example");
        cloudEvent.setSpecversion("1");
        cloudEvent.setDatacontenttype("application/json");
        String val = String.format("This is message #%d", i);
        cloudEvent.setData(new HashMap<>() {
          {
            put("dataKey", val);
          }
        });
        BulkPublishEntry<CloudEvent<Map<String, String>>> entry = new BulkPublishEntry<>(
            "" + (i + 1), cloudEvent, CloudEvent.CONTENT_TYPE, null);
        entries.add(entry);
      }
      BulkPublishRequest<CloudEvent<Map<String, String>>> request = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
          entries);
      BulkPublishResponse<?> res = client.publishEvents(request).block();
      if (res != null) {
        if (res.getFailedEntries().size() > 0) {
          // Ideally this condition will not happen in examples
          System.out.println("Some events failed to be published");
          for (BulkPublishResponseFailedEntry<?> entry : res.getFailedEntries()) {
            System.out.println("EntryId : " + entry.getEntry().getEntryId()
                + " Error message : " + entry.getErrorMessage());
          }
        }
      } else {
        throw new Exception("null response");
      }
      System.out.println("Done");
    }
  }
}

