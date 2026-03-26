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

package io.dapr.it.binding.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class InputBindingController {

  private static final List<String> messagesReceived = Collections.synchronizedList(new ArrayList());

  private static final AtomicBoolean initialized = new AtomicBoolean(false);

  @PostMapping(path = "/sample123")
  @PutMapping(path = "/sample123")
  public void handleInputBinding(@RequestBody(required = false) String body) {
    if ("\"ping\"".equals(body)) {
      // Initialization messages are useful to detect if input binding is up.
      initialized.set(true);
      System.out.println("Input binding is up: " + body);
      return;
    }

    messagesReceived.add(body);
    System.out.println("Received message through binding: " + (body == null ? "" : body));
  }

  @GetMapping(path = "/messages")
  public List<String> getMessages() {
    return messagesReceived;
  }

  @PostMapping(path = "/messages/clear")
  public void clearMessages() {
    messagesReceived.clear();
    initialized.set(false);
  }

  @GetMapping(path = "/")
  public String hello() {
    return "hello";
  }

  @GetMapping(path = "/health")
  public void health() {
  }

  @GetMapping(path = "/github404")
  public ResponseEntity<Map<String, String>> github404() {
    return ResponseEntity.status(404).body(Map.of(
        "message", "Not Found",
        "documentation_url", "https://docs.github.com/rest"));
  }

  @GetMapping(path = "/initialized")
  public void initialized() {
    if (!initialized.get()) {
      throw new RuntimeException("Input binding is not initialized yet.");
    }
  }

}
