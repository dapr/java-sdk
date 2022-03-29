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
