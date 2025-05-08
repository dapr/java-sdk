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

package io.dapr.it.spring.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRestController {

  public static final String pubSubName = "pubsub";
  public static final String topicName = "mockTopic";

  @GetMapping("/ready")
  public String ok() {
    return "OK";
  }

  @GetMapping("/hello")
  public String hello() {
    return "hello";
  }

  @PostMapping("/echo")
  public String echo(@RequestBody String input) {
    return input;
  }

  @PostMapping("/echoj")
  public Result echoJson(@RequestBody String input) {
    return new Result(input);
  }
}
