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

  private static final List<CloudEvent> messagesReceivedTestingTopic = new ArrayList();
  private static final List<CloudEvent> messagesReceivedBinaryTopic = new ArrayList();
  private static final List<CloudEvent> messagesReceivedAnotherTopic = new ArrayList();
  private static final List<CloudEvent> messagesReceivedTTLTopic = new ArrayList();

  @GetMapping(path = "/messages/testingtopic")
  public List<CloudEvent> getMessagesReceivedTestingTopic() {
    return messagesReceivedTestingTopic;
  }

  @GetMapping(path = "/messages/binarytopic")
  public List<CloudEvent> getMessagesReceivedBinaryTopic() {
    return messagesReceivedBinaryTopic;
  }

  @GetMapping(path = "/messages/anothertopic")
  public List<CloudEvent> getMessagesReceivedAnotherTopic() {
    return messagesReceivedAnotherTopic;
  }

  @GetMapping(path = "/messages/ttltopic")
  public List<CloudEvent> getMessagesReceivedTTLTopic() {
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
        messagesReceivedTestingTopic.add(envelope);
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
        messagesReceivedBinaryTopic.add(envelope);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Topic(name = "anothertopic", pubsubName = "messagebus")
  @PostMapping(path = "/route3")
  public Mono<Void> handleMessageAnotherTopic(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        System.out.println("Another topic Subscriber got message: " + message);
        messagesReceivedAnotherTopic.add(envelope);
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
        messagesReceivedTTLTopic.add(envelope);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
