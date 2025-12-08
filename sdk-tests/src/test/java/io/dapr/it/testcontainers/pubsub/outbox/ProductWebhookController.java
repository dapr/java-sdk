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
package io.dapr.it.testcontainers.pubsub.outbox;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/webhooks/products")
public class ProductWebhookController {

  public static final List<CloudEvent<Product>> EVENT_LIST = new CopyOnWriteArrayList<>();

  @PostMapping("/created")
  @Topic(name = "product.created", pubsubName = "pubsub")
  public void handleEvent(@RequestBody CloudEvent cloudEvent) {
    System.out.println("Received product.created event: " + cloudEvent.getData());
    EVENT_LIST.add(cloudEvent);
  }
}
