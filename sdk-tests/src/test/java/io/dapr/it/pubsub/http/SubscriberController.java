/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import io.dapr.serializer.DefaultObjectSerializer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class SubscriberController {

  private static final List<String> messagesReceivedTestingTopic = new ArrayList();
  private static final List<String> messagesReceivedAnotherTopic = new ArrayList();

  @GetMapping(path = "/messages/testingtopic")
  public List<String> getMessagesReceivedTestingTopic() {
    return messagesReceivedTestingTopic;
  }

  @GetMapping(path = "/messages/anothertopic")
  public List<String> getMessagesReceivedAnotherTopic() {
    return messagesReceivedAnotherTopic;
  }

  @Topic(name = "testingtopic", pubsubName = "messagebus")
  @PostMapping(path = "/route1")
  public Mono<Void> handleMessage(@RequestBody(required = false) byte[] body,
                                  @RequestHeader Map<String, String> headers) {
    return Mono.fromRunnable(() -> {
      try {
        // Dapr's event is compliant to CloudEvent.
        CloudEvent envelope = CloudEvent.deserialize(body);

        String message = envelope.getData() == null ? "" : envelope.getData();
        System.out.println("Testing topic Subscriber got message: " + message);
        messagesReceivedTestingTopic.add(envelope.getData());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "anothertopic", pubsubName = "messagebus")
  @PostMapping(path = "/route2")
  public Mono<Void> handleMessageAnotherTopic(@RequestBody(required = false) byte[] body,
                                  @RequestHeader Map<String, String> headers) {
    return Mono.fromRunnable(() -> {
      try {
        // Dapr's event is compliant to CloudEvent.
        CloudEvent envelope = CloudEvent.deserialize(body);

        String message = envelope.getData() == null ? "" : envelope.getData();
        System.out.println("Another topic Subscriber got message: " + message);
        messagesReceivedAnotherTopic.add(envelope.getData());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
