/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.bindings.http;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class InputBindingController {

  @PostMapping(path = "/sample123")
  public Mono<Void> handleInputBinding(@RequestBody(required = false) byte[] body) {
    return Mono.fromRunnable(() ->
      System.out.println("Received message through binding: " + (body == null ? "" : new String(body))));
  }

}
