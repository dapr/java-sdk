/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.binding.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class InputBindingController {

  private static final List<byte[]> messagesReceived = new ArrayList();

  @PostMapping(path = "/sample123")
  public Mono<Void> handleInputBinding(@RequestBody(required = false) byte[] body) {

    return Mono.fromRunnable(() -> {
      messagesReceived.add(body);
      System.out.println("Received message through binding: " + (body == null ? "" : new String(body)));
    });
  }

  @GetMapping(path = "/messages")
  public Mono<List<byte[]>> getMessages() {
    return Mono.just(messagesReceived);
  }


}
