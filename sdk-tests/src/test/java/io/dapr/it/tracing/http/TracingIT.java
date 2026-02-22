package io.dapr.it.tracing.http;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.it.tracing.Validation;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;

@DaprSpringBootTest(classes = Service.class)
@Tag("testcontainers")
public class TracingIT {

  private static final String APP_ID = "tracingithttp-service";

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest(APP_ID);

  @BeforeEach
  public void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
  }

  @Test
  public void testInvoke() throws Exception {
    OpenTelemetry openTelemetry = createOpenTelemetry(OpenTelemetryConfig.SERVICE_NAME);
    Tracer tracer = openTelemetry.getTracer(OpenTelemetryConfig.TRACER_NAME);
    String spanName = UUID.randomUUID().toString();
    Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();

    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.waitForSidecar(10000).block();
      try (Scope scope = span.makeCurrent()) {
        client.invokeMethod(APP_ID, "sleep", 1, HttpExtension.POST)
            .contextWrite(getReactorContext(openTelemetry))
            .block();
      }
    }

    span.end();
    Validation.validate(spanName, "calllocal/tracingithttp-service/sleep");
  }
}
