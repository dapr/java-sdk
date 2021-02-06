/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.examples;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

@Component
public class OpenTelemetryInterceptor implements HandlerInterceptor {

  @Autowired
  private OpenTelemetry openTelemetry;

  private static final TextMapPropagator.Getter<HttpServletRequest> HTTP_SERVLET_REQUEST_GETTER =
      new TextMapPropagator.Getter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
          return Collections.list(carrier.getHeaderNames());
        }

        @Nullable
        @Override
        public String get(@Nullable HttpServletRequest carrier, String key) {
          return carrier.getHeader(key);
      }
  };

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    final TextMapPropagator textFormat = openTelemetry.getPropagators().getTextMapPropagator();
    // preHandle is called twice for asynchronous request. For more information, read:
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/AsyncHandlerInterceptor.html
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      return true;
    }

    Context context = textFormat.extract(Context.current(), request, HTTP_SERVLET_REQUEST_GETTER);
    request.setAttribute("opentelemetry-context", context);
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
  }

}
