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

import io.dapr.config.Properties;
import io.dapr.examples.OpenTelemetryInterceptor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * SpringBoot Controller to handle service invocation.
 *
 * <p>Instrumentation is handled in {@link OpenTelemetryInterceptor}.
 */
@RestController
public class TracingDemoMiddleServiceController {

  private static final String INVOKE_APP_ID = "tracingdemo";

  private static final HttpClient httpClient = HttpClient.newHttpClient();

  /**
   * Handles the 'echo' method invocation, by proxying a call into another service.
   *
   * @param context The tracing context for the request.
   * @param body The body of the http message.
   * @return A message containing the time.
   */
  @PostMapping(path = "/proxy_echo")
  public Mono<byte[]> echo(
      @RequestAttribute(name = "opentelemetry-context") Context context,
      @RequestBody(required = false) String body) {
    return Mono.fromFuture(() -> {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(buildInvokeUrl("echo")))
          .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
      injectTraceContext(builder, context);
      addDaprApiToken(builder);
      return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }).map(HttpResponse::body);
  }

  /**
   * Handles the 'sleep' method invocation, by proxying a call into another service.
   *
   * @param context The tracing context for the request.
   * @return A message containing the time.
   */
  @PostMapping(path = "/proxy_sleep")
  public Mono<Void> sleep(@RequestAttribute(name = "opentelemetry-context") Context context) {
    return Mono.fromFuture(() -> {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(buildInvokeUrl("sleep")))
          .POST(HttpRequest.BodyPublishers.noBody());
      injectTraceContext(builder, context);
      addDaprApiToken(builder);
      return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding());
    }).then();
  }

  private static String buildInvokeUrl(String method) {
    int port = Properties.HTTP_PORT.get();
    return "http://localhost:" + port + "/v1.0/invoke/" + INVOKE_APP_ID + "/method/" + method;
  }

  private static void injectTraceContext(HttpRequest.Builder builder, Context context) {
    TextMapSetter<HttpRequest.Builder> setter = HttpRequest.Builder::header;
    GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
        .inject(context, builder, setter);
  }

  private static void addDaprApiToken(HttpRequest.Builder builder) {
    String token = Properties.API_TOKEN.get();
    if (token != null) {
      builder.header("dapr-api-token", token);
    }
  }

}
