/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.pubsub.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
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
  @Topic(name = "testingtopic", pubsubName = "messagebus")
  @PostMapping(path = "/testingtopic")
  public Mono<Void> handleMessage(@RequestBody(required = false) CloudEvent cloudEvent) {
    return Mono.fromRunnable(() -> {
      try {
        System.out.println("Subscriber got: " + cloudEvent.getData());
        System.out.println("Subscriber got: " + OBJECT_MAPPER.writeValueAsString(cloudEvent));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
