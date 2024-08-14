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

package io.dapr.it.testcontainers;

import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SubscriptionsRestController {

  private final List<CloudEvent<?>> events = new ArrayList<>();

  @PostMapping(path = "/events", consumes = "application/cloudevents+json")
  public void receiveEvents(@RequestBody CloudEvent<?> event) {
    events.add(event);
  }

  @GetMapping(path = "/events", produces = "application/cloudevents+json")
  public List<CloudEvent<?>> getAllEvents() {
    return events;
  }

}
