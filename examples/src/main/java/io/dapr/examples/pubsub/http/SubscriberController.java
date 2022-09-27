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

package io.dapr.examples.pubsub.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.Rule;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import io.dapr.springboot.DaprBulkMessage;
import io.dapr.springboot.DaprBulkMessageEntry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class SubscriberController {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Handles a registered publish endpoint on this app.
   * @param cloudEvent The cloud event received.
   * @return A message containing the time.
   */
  @Topic(name = "testingtopic", pubsubName = "${myAppProperty:pubsub}")
  @PostMapping(path = "/testingtopic")
  public Mono<Void> handleMessage(@RequestBody(required = false) CloudEvent<String> cloudEvent) {
    return Mono.fromRunnable(() -> {
      try {
        System.out.println("Subscriber got: " + cloudEvent.getData());
        System.out.println("Subscriber got: " + OBJECT_MAPPER.writeValueAsString(cloudEvent));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Handles a registered publish endpoint on this app (version 2 of a cloud event).
   * @param cloudEvent The cloud event received.
   * @return A message containing the time.
   */
  @Topic(name = "testingtopic", pubsubName = "${myAppProperty:pubsub}",
          rule = @Rule(match = "event.type == \"v2\"", priority = 1))
  @PostMapping(path = "/testingtopicV2")
  public Mono<Void> handleMessageV2(@RequestBody(required = false) CloudEvent cloudEvent) {
    return Mono.fromRunnable(() -> {
      try {
        System.out.println("Subscriber got: " + cloudEvent.getData());
        System.out.println("Subscriber got: " + OBJECT_MAPPER.writeValueAsString(cloudEvent));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Handles a registered subscribe endpoint on this app using bulk subscribe.
   * @param bulkMessage The bulk message received.
   * @return A list of responses for each event. TODO
   */
  @Topic(name = "testingtopicbulk", pubsubName = "${myAppProperty:pubsub}", bulk = "true")
  @PostMapping(path = "/testingtopicbulk")
  public Mono<Void> handleBulkMessage(@RequestBody(required = false) DaprBulkMessage bulkMessage) {
    return Mono.fromRunnable(() -> {
      try {
        System.out.printf("Subscriber got %d messages\n", bulkMessage.entries.length);
        for (DaprBulkMessageEntry<?> entry: bulkMessage.entries) {
          System.out.printf("Entry ID: %s\n", entry.entryID);
          System.out.printf("Event: %s\n", entry.event);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
