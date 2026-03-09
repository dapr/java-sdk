/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.springboot4.examples.consumer;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SubscriberRestController {

  private final Logger logger = LoggerFactory.getLogger(SubscriberRestController.class);

  private List<CloudEvent> events = new ArrayList<>();

  /**
   * Subscribe to cloud events.
   * @param cloudEvent payload
   */
  @PostMapping("subscribe")
  @Topic(pubsubName = "pubsub", name = "topic")
  public void subscribe(@RequestBody CloudEvent<Order> cloudEvent) {
    logger.info("Order Event Received: " + cloudEvent.getData());
    events.add(cloudEvent);
  }

  @GetMapping("events")
  public List<CloudEvent> getAllEvents() {
    return events;
  }

}

