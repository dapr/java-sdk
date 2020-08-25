/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class OpenTelemetryInterceptor implements HandlerInterceptor {

  @Autowired
  Tracer tracer;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    final HttpTextFormat textFormat = OpenTelemetry.getPropagators().getHttpTextFormat();
    // preHandle is called twice for asynchronous request. For more information, read:
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/AsyncHandlerInterceptor.html
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      return true;
    }

    Span span;
    try {
      Context context = textFormat.extract(
          Context.current(),
          request,
          new HttpTextFormat.Getter<HttpServletRequest>() {
            @Override
            public String get(HttpServletRequest req, String key) {
              return req.getHeader(key);
            }
          });
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
    final HttpTextFormat textFormat = OpenTelemetry.getPropagators().getHttpTextFormat();
    textFormat.inject(context, response,
        new HttpTextFormat.Setter<HttpServletResponse>() {
          @Override
          public void set(HttpServletResponse response, String key, String value) {
            response.addHeader(key, value);
          }
        });
    span.end();
  }

}
