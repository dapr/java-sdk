/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
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
  @Autowired
  Tracer tracer;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    final TextMapPropagator textFormat = OpenTelemetry.getGlobalPropagators().getTextMapPropagator();
    // preHandle is called twice for asynchronous request. For more information, read:
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/AsyncHandlerInterceptor.html
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      return true;
    }

    Span span;
    try {
      Context context = textFormat.extract(Context.current(), request, HTTP_SERVLET_REQUEST_GETTER);
      request.setAttribute("opentelemetry-context", context);
      span = tracer.spanBuilder(request.getRequestURI()).setParent(context).startSpan();
      span.setAttribute("handler", "pre");
    } catch (Exception e) {
      span = tracer.spanBuilder(request.getRequestURI()).startSpan();
      span.setAttribute("handler", "pre");

      span.addEvent(e.toString());
      span.setAttribute("error", true);
    }
    request.setAttribute("opentelemetry-span", span);
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception exception) {
    Object contextObject = request.getAttribute("opentelemetry-context");
    Object spanObject = request.getAttribute("opentelemetry-span");
    if ((contextObject == null) || (spanObject == null)) {
      return;
    }
    Context context = (Context) contextObject;
    Span span = (Span) spanObject;
    span.setAttribute("handler", "afterCompletion");
    final TextMapPropagator textFormat = OpenTelemetry.getGlobalPropagators().getTextMapPropagator();
    textFormat.inject(context, response, HttpServletResponse::addHeader);
    span.end();
  }

}
