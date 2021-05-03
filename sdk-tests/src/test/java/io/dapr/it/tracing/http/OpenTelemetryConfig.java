/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.tracing.http;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class OpenTelemetryConfig {

  public static final String TRACER_NAME = "integration testing tracer";

  public static final String SERVICE_NAME = "integration testing service over http";

  @Bean
  public OpenTelemetry initOpenTelemetry() {
    return io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry(SERVICE_NAME);
  }

  @Bean
  public Tracer initTracer(@Autowired OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(TRACER_NAME);
  }

}
