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

package io.dapr.springboot.examples.openfeign.client;

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import io.dapr.springboot.examples.openfeign.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "producer-client", url = "http://method.producer-app/")
@UseDaprClient
public interface ProducerClient {

  @PostMapping("/orders")
  String storeOrder(@RequestBody Order order);

  @GetMapping(value = "/orders", produces = "application/json")
  Iterable<Order> getAll();

  @GetMapping(value = "/orders/byItem/", produces = "application/json")
  Iterable<Order> getAllByItem(@RequestParam("item") String item);
}
