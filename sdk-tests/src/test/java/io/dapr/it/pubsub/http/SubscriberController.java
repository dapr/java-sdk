/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

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

  private static final List<String> messagesReceived = new ArrayList();

  /**
   * Dapr's default serializer/deserializer.
   */
  private static final DefaultObjectSerializer SERIALIZER = new DefaultObjectSerializer ();

  @GetMapping("/dapr/subscribe")
  public byte[] daprConfig() throws Exception {
    return SERIALIZER.serialize(new String[] { "testingtopic" });
  }

  @GetMapping(path = "/messages")
  public List<String> getMessages() {
    return messagesReceived;
  }

  @PostMapping(path = "/testingtopic")
  public Mono<Void> handleMessage(@RequestBody(required = false) byte[] body,
                                  @RequestHeader Map<String, String> headers) {
    return Mono.fromRunnable(() -> {
      try {
        // Dapr's event is compliant to CloudEvent.
        CloudEvent envelope = CloudEvent.deserialize(body);

        String message = envelope.getData() == null ? "" : envelope.getData();
        System.out.println("Subscriber got message: " + message);
        messagesReceived.add(envelope.getData());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
