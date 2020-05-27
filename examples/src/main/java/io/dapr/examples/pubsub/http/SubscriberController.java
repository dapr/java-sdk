/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.pubsub.http;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class SubscriberController {

  /**
   * Handles a registered publish endpoint on this app.
   * @param body The body of the http message.
   * @param headers The headers of the http message.
   * @return A message containing the time.
   */
  @Topic(name = "testingtopic")
  @PostMapping(path = "/testingtopic")
  public Mono<Void> handleMessage(@RequestBody(required = false) byte[] body,
                                  @RequestHeader Map<String, String> headers) {
    return Mono.fromRunnable(() -> {
      try {
        // Dapr's event is compliant to CloudEvent.
        CloudEvent envelope = CloudEvent.deserialize(body);

        String message = envelope.getData() == null ? "" : new String(envelope.getData());
        System.out.println("Subscriber got message: " + message);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
