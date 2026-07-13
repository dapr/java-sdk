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
  public OpenTelemetry initOpenTelemetry() {
    // Use the explicit-endpoint overload so this bean does NOT block on a Zipkin readiness
    // probe during app startup. This app runs as a host subprocess and only needs a propagator
    // for context extraction; it exports no spans that the test asserts on (the validated
    // "calllocal/<app>/sleep" span is emitted by the Dapr sidecar, which exports to Zipkin over
    // the container network). The legacy createOpenTelemetry(SERVICE_NAME) overload probed
    // 127.0.0.1:9411, but Zipkin now runs as a Testcontainer on a random mapped port, so the
    // probe failed and crashed startup -- startAppAndAttach then saw "connection refused".
    return io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry(
        SERVICE_NAME, "http://localhost:9411/api/v2/spans");
  }

  @Bean
  public Tracer initTracer(@Autowired OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(TRACER_NAME);
  }

}
