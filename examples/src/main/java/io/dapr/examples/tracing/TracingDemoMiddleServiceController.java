/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.tracing;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.InvokeMethodRequestBuilder;
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
    InvokeMethodRequestBuilder builder = new InvokeMethodRequestBuilder(INVOKE_APP_ID, "echo");
    InvokeMethodRequest request = builder
        .withBody(body)
        .withHttpExtension(HttpExtension.POST)
        .withContext(getReactorContext(context)).build();
    return client.invokeMethod(request, TypeRef.get(byte[].class)).map(r -> r.getObject());
  }

  /**
   * Handles the 'sleep' method invocation, by proxying a call into another service.
   *
   * @param context The tracing context for the request.
   * @return A message containing the time.
   */
  @PostMapping(path = "/proxy_sleep")
  public Mono<Void> sleep(@RequestAttribute(name = "opentelemetry-context") Context context) {
    InvokeMethodRequestBuilder builder = new InvokeMethodRequestBuilder(INVOKE_APP_ID, "sleep");
    InvokeMethodRequest request = builder
        .withHttpExtension(HttpExtension.POST)
        .withContext(getReactorContext(context)).build();
    return client.invokeMethod(request, TypeRef.get(byte[].class)).then();
  }

}
