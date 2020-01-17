/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.pubsub.http;

import io.dapr.client.domain.CloudEventEnvelope;
import io.dapr.utils.ObjectSerializer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class SubscriberController {

  /**
   * Dapr's default serializer/deserializer.
   */
  private static final ObjectSerializer SERIALIZER = new ObjectSerializer();

  @GetMapping("/dapr/subscribe")
  public byte[] daprConfig() throws Exception {
    return SERIALIZER.serialize(new String[] { "message" });
  }

  @PostMapping(path = "/message")
  public Mono<Void> handleMessage(@RequestBody(required = false) byte[] body,
                                   @RequestHeader Map<String, String> headers) {
    return Mono.fromRunnable(() -> {
      try {
        // Dapr's event is compliant to CloudEvent.
        CloudEventEnvelope envelope = SERIALIZER.deserialize(body, CloudEventEnvelope.class);

        String message = envelope.getData() == null ? "" : new String(envelope.getData());
        System.out.println("Subscriber got message: " + message);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
