/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.it.spring.messaging;

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
public class TestRestController {

  public static final String pubSubName = "pubsub";
  public static final String topicName = "mockTopic";
  private static final Logger LOG = LoggerFactory.getLogger(TestRestController.class);
  private final List<CloudEvent<String>> events = new ArrayList<>();

  @GetMapping("/")
  public String ok() {
    return "OK";
  }

  @Topic(name = topicName, pubsubName = pubSubName)
  @PostMapping("/subscribe")
  public void handleMessages(@RequestBody CloudEvent<String> event) {
    LOG.info("++++++CONSUME {}------", event);
    events.add(event);
  }

  public List<CloudEvent<String>> getEvents() {
    return events;
  }

}
