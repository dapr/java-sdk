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

package io.dapr.it.tracing.http;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

@Component
public class OpenTelemetryInterceptor implements HandlerInterceptor {

  @Autowired
  private OpenTelemetry openTelemetry;

  // Implementation for springboot 3.0, which uses jakarta.servlet instead of javax.servlet
  private static final TextMapGetter<HttpServletRequest> JAKARTA_HTTP_SERVLET_REQUEST_GETTER =
      new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
          return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
          return carrier.getHeader(key);
      }
  };

  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    final TextMapPropagator textFormat = openTelemetry.getPropagators().getTextMapPropagator();
    // preHandle is called twice for asynchronous request. For more information, read:
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/AsyncHandlerInterceptor.html
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      return true;
    }

    Context context = textFormat.extract(Context.current(), request, JAKARTA_HTTP_SERVLET_REQUEST_GETTER);
    request.setAttribute("opentelemetry-context", context);
    return true;
  }

  public void postHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
    // There is no global context to be changed in post handle since it is done in preHandle on a new call.
  }

  
  // Implementation for springboot 3.0, which uses jakarta.servlet instead of javax.servlet
  private static final TextMapGetter<javax.servlet.http.HttpServletRequest> JAVA_HTTP_SERVLET_REQUEST_GETTER =
      new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(javax.servlet.http.HttpServletRequest carrier) {
          return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(javax.servlet.http.HttpServletRequest carrier, String key) {
          return carrier.getHeader(key);
      }
  };

  public boolean preHandle(
      javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Object handler) {
    final TextMapPropagator textFormat = openTelemetry.getPropagators().getTextMapPropagator();
    // preHandle is called twice for asynchronous request. For more information, read:
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/AsyncHandlerInterceptor.html
    if (request.getDispatcherType() == javax.servlet.DispatcherType.ASYNC) {
      return true;
    }

    Context context = textFormat.extract(Context.current(), request, JAVA_HTTP_SERVLET_REQUEST_GETTER);
    request.setAttribute("opentelemetry-context", context);
    return true;
  }

  public void postHandle(
      javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
    // There is no global context to be changed in post handle since it is done in preHandle on a new call.
  }

}
