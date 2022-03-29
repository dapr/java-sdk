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

package io.dapr.examples.tracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.examples.OpenTelemetryInterceptor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

/**
 * SpringBoot Controller to handle service invocation.
 *
 * <p>Instrumentation is handled in {@link OpenTelemetryInterceptor}.
 */
@RestController
public class TracingDemoServiceController {

  /**
   * Json serializer/deserializer.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Format to output date and time.
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Handles the 'echo' method invocation.
   *
   * @param body The body of the http message.
   * @param headers The headers of the http message.
   * @return A message containing the time.
   */
  @PostMapping(path = "/echo")
  public Mono<String> handleMethod(@RequestBody(required = false) String body,
                                   @RequestHeader Map<String, String> headers) {
    return Mono.fromSupplier(() -> {
      try {
        String message = body == null ? "" : body;

        Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

        String metadataString = headers == null ? "" : OBJECT_MAPPER.writeValueAsString(headers);

        // Handles the request by printing message.
        System.out.println(
            "Server: " + message + " @ " + utcNowAsString + " and metadata: " + metadataString);

        return utcNowAsString;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Handles the 'sleep' method invocation.
   */
  @PostMapping(path = "/sleep")
  public void sleep() throws Exception {
    // Simulate slow processing for metrics.
    Thread.sleep(1000);
  }

}
