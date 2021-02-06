/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class SubscriberController {

  private static final List<Object> messagesReceivedTestingTopic = new ArrayList();
  private static final List<Object> messagesReceivedAnotherTopic = new ArrayList();
  private static final List<Object> messagesReceivedTTLTopic = new ArrayList();

  @GetMapping(path = "/messages/testingtopic")
  public List<Object> getMessagesReceivedTestingTopic() {
    return messagesReceivedTestingTopic;
  }

  @GetMapping(path = "/messages/anothertopic")
  public List<Object> getMessagesReceivedAnotherTopic() {
    return messagesReceivedAnotherTopic;
  }

  @GetMapping(path = "/messages/ttltopic")
  public List<Object> getMessagesReceivedTTLTopic() {
    return messagesReceivedTTLTopic;
  }

  @Topic(name = "testingtopic", pubsubName = "messagebus")
  @PostMapping(path = "/route1")
  public Mono<Void> handleMessage(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesReceivedTestingTopic.add(envelope.getData());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "anothertopic", pubsubName = "messagebus")
  @PostMapping(path = "/route2")
  public Mono<Void> handleMessageAnotherTopic(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        System.out.println("Another topic Subscriber got message: " + message);
        messagesReceivedAnotherTopic.add(envelope.getData());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "ttltopic", pubsubName = "messagebus")
  @PostMapping(path = "/route3")
  public Mono<Void> handleMessageTTLTopic(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        System.out.println("TTL topic Subscriber got message: " + message);
        messagesReceivedTTLTopic.add(envelope.getData());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
