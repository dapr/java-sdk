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

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
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

  @Topic(name = "testingtopic", pubsubName = "messagebus")
  @PostMapping("/route1")
  public Mono<Void> handleMessage(@RequestBody(required = false) CloudEvent envelope) {
    return Mono.fromRunnable(() -> {
      try {
        String message = envelope.getData() == null ? "" : envelope.getData().toString();
        String contentType = envelope.getDatacontenttype() == null ? "" : envelope.getDatacontenttype();
        System.out.println("Testing topic Subscriber got message: " + message + "; Content-type: " + contentType);
        messagesByTopic.compute("testingtopic", merge(envelope));
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

  private BiFunction<String, List<CloudEvent<?>>, List<CloudEvent<?>>> merge(final CloudEvent<?> item) {
    return (key, value) -> {
      final List<CloudEvent<?>> list = value == null ? new ArrayList<>() : value;
      list.add(item);
      return list;
    };
  }
}
