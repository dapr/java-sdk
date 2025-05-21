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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

import static io.dapr.it.spring.feign.DaprFeignIT.BINDING_NAME;

@FeignClient(value = "postgres-binding", url = "http://binding." + BINDING_NAME)
@UseDaprClient
public interface PostgreBindingClient {

  @PostMapping("/exec")
  void exec(@RequestHeader("sql") String sql, @RequestHeader("params") List<String> params);

  @PostMapping("/exec")
  void exec(@RequestHeader("sql") String sql, @RequestHeader("params") String params);

  @PostMapping("/query")
  String query(@RequestHeader("sql") String sql, @RequestHeader("params") List<String> params);
}
