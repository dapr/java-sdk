/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.boot.pubsub.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SubscriberController {

  //The name of topic
  private static final String TOPIC_NAME = "testingtopic";

  //The name of the pubsub
  private static final String PUBSUB_NAME = "pubsub";

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * 1.0.0-rc2的时候cloudEvent还没有无参构造方法，直接放在请求参数里面会运行时报错，因为jackson无法创建对象
   * Handles a registered publish endpoint on this app.
   * @param body The cloud event received.
   * @return A message containing the time.
   */
  @Topic(name = TOPIC_NAME, pubsubName = PUBSUB_NAME)
  @PostMapping(path = "/" + TOPIC_NAME)
  public Mono<Void> handleMessage(@RequestBody(required = false) byte[] body) {
    return Mono.fromRunnable(() -> {
      try {
        CloudEvent envelope = CloudEvent.deserialize(body);
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        System.out.println("Subscriber got: " + objectMapper.writeValueAsString(envelope));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
