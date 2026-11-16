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

package io.dapr.it.testcontainers.binding.http.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class InputBindingController {

  private final List<String> messagesReceived = new ArrayList<>();

  private boolean initialized;

  @PostMapping(path = "/sample123")
  @PutMapping(path = "/sample123")
  public synchronized void handleInputBinding(@RequestBody(required = false) String body) {
    if ("\"ping\"".equals(body)) {
      // Initialization messages are useful to detect if input binding is up.
      initialized = true;
      System.out.println("Input binding is up: " + body);
      return;
    }

    messagesReceived.add(body);
    System.out.println("Received message through binding: " + (body == null ? "" : body));
  }

  @GetMapping(path = "/messages")
  public synchronized List<String> getMessages() {
    return new ArrayList<>(messagesReceived);
  }

  @GetMapping(path = "/")
  public String hello() {
    return "hello";
  }

  @GetMapping(path = "/health")
  public void health() {
  }

  @GetMapping(path = "/initialized")
  public synchronized boolean initialized() {
    return initialized;
  }

  @PostMapping(path = "/reset")
  public synchronized void reset() {
    messagesReceived.clear();
    initialized = false;
  }

  @GetMapping(path = "/fixture/upstream/not-found")
  public ResponseEntity<Map<String, String>> upstreamNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("marker", "binding-fixture-not-found"));
  }

}
