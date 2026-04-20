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
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

  public static final String TRACER_NAME = "integration testing tracer";

  public static final String SERVICE_NAME = "integration testing service over http";

  @Bean
  public OpenTelemetry initOpenTelemetry() throws InterruptedException {
    return io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry(SERVICE_NAME);
  }

  @Bean
  public Tracer initTracer(@Autowired OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(TRACER_NAME);
  }

}
