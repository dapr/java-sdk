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

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "test-method", url = "http://method.dapr-feign-test/")
@UseDaprClient
public interface TestMethodClient {

  @GetMapping(value = "/hello")
  String hello();

  @PostMapping("/echo")
  String echo(@RequestBody String input);

  @PostMapping(value = "/echoj", produces = "application/json;charset=utf-8")
  Result echoJson(@RequestBody String input);

}
