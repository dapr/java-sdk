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

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.examples.OpenTelemetryInterceptor;
import io.dapr.utils.TypeRef;
import io.opentelemetry.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static io.dapr.examples.OpenTelemetryConfig.getReactorContext;

/**
 * SpringBoot Controller to handle service invocation.
 *
 * <p>Instrumentation is handled in {@link OpenTelemetryInterceptor}.
 */
@RestController
public class TracingDemoMiddleServiceController {

  private static final String INVOKE_APP_ID = "tracingdemo";

  /**
   * Dapr client.
   */
  @Autowired
  private DaprClient client;

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
    InvokeMethodRequest request = new InvokeMethodRequest(INVOKE_APP_ID, "echo")
        .setBody(body)
        .setHttpExtension(HttpExtension.POST);
    return client.invokeMethod(request, TypeRef.get(byte[].class)).contextWrite(getReactorContext(context));
  }

  /**
   * Handles the 'sleep' method invocation, by proxying a call into another service.
   *
   * @param context The tracing context for the request.
   * @return A message containing the time.
   */
  @PostMapping(path = "/proxy_sleep")
  public Mono<Void> sleep(@RequestAttribute(name = "opentelemetry-context") Context context) {
    InvokeMethodRequest request = new InvokeMethodRequest(INVOKE_APP_ID, "sleep")
        .setHttpExtension(HttpExtension.POST);
    return client.invokeMethod(request, TypeRef.get(byte[].class)).contextWrite(getReactorContext(context)).then();
  }

}
