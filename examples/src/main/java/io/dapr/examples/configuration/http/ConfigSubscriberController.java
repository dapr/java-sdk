/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.examples.configuration.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.Map;

/**
 * Spring boot Controller class for api endpoints.
 */
@RestController
public class ConfigSubscriberController {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Api mapping for subscribe configuration.
   * @param pathVarsMap Path variables for post call
   * @param obj request Body
   * @return Returns void
   */
  @PostMapping(path = "/configuration/{configStore}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Void> handleMessage(@PathVariable Map<String, String> pathVarsMap, @RequestBody JsonNode obj) {
    return Mono.fromRunnable(
      () -> {
        try {
          for (Iterator<JsonNode> it = obj.get("items").elements(); it.hasNext(); ) {
            JsonNode node = it.next();
            String key = node.path("key").asText();
            String value = node.path("value").asText();
            System.out.println(value + " : key ->" + key);
          }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
      }
    );
  }
}
