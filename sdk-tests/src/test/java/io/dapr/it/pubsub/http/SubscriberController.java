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

package io.dapr.it.pubsub.http;

import io.dapr.Rule;
import io.dapr.Topic;
import io.dapr.client.domain.BulkSubscribeAppResponse;
import io.dapr.client.domain.BulkSubscribeAppResponseEntry;
import io.dapr.client.domain.BulkSubscribeAppResponseStatus;
import io.dapr.client.domain.BulkSubscribeMessage;
import io.dapr.client.domain.BulkSubscribeMessageEntry;
import io.dapr.client.domain.CloudEvent;
import io.dapr.springboot.annotations.BulkSubscribe;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class SubscriberController {

  private final Map<String, List<CloudEvent<?>>> messagesByTopic = Collections.synchronizedMap(new HashMap<>());

  @GetMapping(path = "/messages/{topic}")
  public List<CloudEvent<?>> getMessagesByTopic(@PathVariable("topic") String topic) {
    return messagesByTopic.getOrDefault(topic, Collections.emptyList());
  }

  private static final List<CloudEvent> messagesReceivedBulkPublishTopic = new ArrayList();
  private static final List<CloudEvent> messagesReceivedTestingTopic = new ArrayList();
  private static final List<CloudEvent> messagesReceivedTestingTopicV2 = new ArrayList();
  private static final List<CloudEvent> messagesReceivedTestingTopicV3 = new ArrayList();
  private static final List<BulkSubscribeAppResponse> responsesReceivedTestingTopicBulkSub = new ArrayList<>();

  @GetMapping(path = "/messages/redis/testingbulktopic")
  public List<CloudEvent> getMessagesReceivedBulkTopic() {
    return messagesReceivedBulkPublishTopic;
  }



  @GetMapping(path = "/messages/testingtopic")
  public List<CloudEvent> getMessagesReceivedTestingTopic() {
    return messagesReceivedTestingTopic;
  }

  @GetMapping(path = "/messages/testingtopicV2")
  public List<CloudEvent> getMessagesReceivedTestingTopicV2() {
    return messagesReceivedTestingTopicV2;
  }

  @GetMapping(path = "/messages/testingtopicV3")
  public List<CloudEvent> getMessagesReceivedTestingTopicV3() {
    return messagesReceivedTestingTopicV3;
  }

  @GetMapping(path = "/messages/topicBulkSub")
  public List<BulkSubscribeAppResponse> getMessagesReceivedTestingTopicBulkSub() {
    return responsesReceivedTestingTopicBulkSub;
  }
  
  @Topic(name = "testingtopic", pubsubName = "messagebus")
  @PostMapping("/route1")
  public Mono<Void> handleMessage(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesReceivedTestingTopic.add(envelope);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "testingbulktopic", pubsubName = "messagebus")
  @PostMapping("/route1_redis")
  public Mono<Void> handleBulkTopicMessage(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing bulk publish topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesReceivedBulkPublishTopic.add(envelope);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "testingtopic", pubsubName = "messagebus",
          rule = @Rule(match = "event.type == 'myevent.v2'", priority = 2))
  @PostMapping(path = "/route1_v2")
  public Mono<Void> handleMessageV2(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesReceivedTestingTopicV2.add(envelope);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "testingtopic", pubsubName = "messagebus",
          rule = @Rule(match = "event.type == 'myevent.v3'", priority = 1))
  @PostMapping(path = "/route1_v3")
  public Mono<Void> handleMessageV3(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesReceivedTestingTopicV3.add(envelope);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "typedtestingtopic", pubsubName = "messagebus")
  @PostMapping(path = "/route1b")
  public Mono<Void> handleMessageTyped(@RequestBody(required = false) CloudEvent<PubSubIT.MyObject> envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String id = envelope.getData() == null ? "" : envelope.getData().getId();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing typed topic Subscriber got message with ID: " + id + "; Content-type: " + contentType);
        messagesByTopic.compute("typedtestingtopic", merge(envelope));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "binarytopic", pubsubName = "messagebus")
  @PostMapping(path = "/route2")
  public Mono<Void> handleBinaryMessage(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Binary topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesByTopic.compute("binarytopic", merge(envelope));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "#{'another'.concat('topic')}", pubsubName = "${pubsubName:messagebus}")
  @PostMapping(path = "/route3")
  public Mono<Void> handleMessageAnotherTopic(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        System.out.println("Another topic Subscriber got message: " + message);
        messagesByTopic.compute("anothertopic", merge(envelope));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "ttltopic", pubsubName = "messagebus")
  @PostMapping(path = "/route4")
  public Mono<Void> handleMessageTTLTopic(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        System.out.println("TTL topic Subscriber got message: " + message);
        messagesByTopic.compute("ttltopic", merge(envelope));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "testinglongvalues", pubsubName = "messagebus")
  @PostMapping(path = "/testinglongvalues")
  public Mono<Void> handleMessageLongValues(@RequestBody(required = false) CloudEvent<PubSubIT.ConvertToLong> cloudEvent) {
    return Mono.fromRunnable(() -> {
      try {
        Long message = cloudEvent.getData().getValue();
        System.out.println("Subscriber got: " + message);
        messagesByTopic.compute("testinglongvalues", merge(cloudEvent));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Receive messages using the bulk subscribe API.
   * The maxBulkSubCount and maxBulkSubAwaitDurationMs are adjusted to ensure
   * that all the test messages arrive in a single batch.
   *
   * @param bulkMessage incoming bulk of messages from the message bus.
   * @return status for each message received.
   */
  @BulkSubscribe(maxMessagesCount = 100, maxAwaitDurationMs = 5000)
  @Topic(name = "topicBulkSub", pubsubName = "messagebus")
  @PostMapping(path = "/routeBulkSub")
  public Mono<BulkSubscribeAppResponse> handleMessageBulk(
          @RequestBody(required = false) BulkSubscribeMessage<CloudEvent<String>> bulkMessage) {
    return Mono.fromCallable(() -> {
      if (bulkMessage.getEntries().size() == 0) {
        BulkSubscribeAppResponse response = new BulkSubscribeAppResponse(new ArrayList<>());
        responsesReceivedTestingTopicBulkSub.add(response);
        return response;
      }

      List<BulkSubscribeAppResponseEntry> entries = new ArrayList<>();
      for (BulkSubscribeMessageEntry<?> entry: bulkMessage.getEntries()) {
        try {
          System.out.printf("Bulk Subscriber got entry ID: %s\n", entry.getEntryId());
          entries.add(new BulkSubscribeAppResponseEntry(entry.getEntryId(), BulkSubscribeAppResponseStatus.SUCCESS));
        } catch (Exception e) {
          entries.add(new BulkSubscribeAppResponseEntry(entry.getEntryId(), BulkSubscribeAppResponseStatus.RETRY));
        }
      }
      BulkSubscribeAppResponse response = new BulkSubscribeAppResponse(entries);
      responsesReceivedTestingTopicBulkSub.add(response);
      return response;
    });
  }

  private BiFunction<String, List<CloudEvent<?>>, List<CloudEvent<?>>> merge(final CloudEvent<?> item) {
    return (key, value) -> {
      final List<CloudEvent<?>> list = value == null ? new ArrayList<>() : value;
      list.add(item);
      return list;
    };
  }

  @GetMapping(path = "/health")
  public void health() {
  }
}
