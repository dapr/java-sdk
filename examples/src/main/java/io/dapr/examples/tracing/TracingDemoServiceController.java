/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
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
