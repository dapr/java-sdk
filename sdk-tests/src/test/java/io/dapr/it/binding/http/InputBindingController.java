/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.binding.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class InputBindingController {

  private static final List<String> messagesReceived = new ArrayList();

  @PostMapping(path = "/sample123")
  @PutMapping(path = "/sample123")
  public void handleInputBinding(@RequestBody(required = false) String body) {
    messagesReceived.add(body);
    System.out.println("Received message through binding: " + (body == null ? "" : body));
  }

  @GetMapping(path = "/messages")
  public List<String> getMessages() {
    return messagesReceived;
  }

  @GetMapping(path = "/")
  public String hello() {
    return "hello";
  }

}
