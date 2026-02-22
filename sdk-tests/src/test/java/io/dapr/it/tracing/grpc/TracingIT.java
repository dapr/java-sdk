package io.dapr.it.tracing.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.tracing.Validation;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprProtocol;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.utils.NetworkUtils;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;

@Testcontainers
@Tag("testcontainers")
public class TracingIT {

  private static final String APP_ID = "tracingitgrpc-service";

  @Container
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory
      .createForSpringBootTest(APP_ID)
      .withAppProtocol(DaprProtocol.GRPC);

  private DaprClient daprClient;

  @BeforeAll
  public static void startGrpcApp() throws Exception {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    Thread appThread = new Thread(() -> {
      try {
        Service.main(new String[] {String.valueOf(DAPR_CONTAINER.getAppPort())});
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    });
    appThread.setDaemon(true);
    appThread.start();
    NetworkUtils.waitForSocket("127.0.0.1", DAPR_CONTAINER.getAppPort(), 30000);
  }

  @BeforeEach
  public void setup() {
    daprClient = new io.dapr.client.DaprClientBuilder()
        .withPropertyOverride(io.dapr.config.Properties.HTTP_ENDPOINT, "http://localhost:" + DAPR_CONTAINER.getHttpPort())
        .withPropertyOverride(io.dapr.config.Properties.GRPC_ENDPOINT, "http://localhost:" + DAPR_CONTAINER.getGrpcPort())
        .build();
    daprClient.waitForSidecar(10000).block();
  }

  @AfterEach
  public void tearDown() throws Exception {
    daprClient.close();
  }

  @Test
  public void testInvoke() throws Exception {
    OpenTelemetry openTelemetry = createOpenTelemetry("service over grpc");
    Tracer tracer = openTelemetry.getTracer("grpc integration test tracer");
    String spanName = UUID.randomUUID().toString();
    Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();

    try (Scope scope = span.makeCurrent()) {
      SleepRequest req = SleepRequest.newBuilder().setSeconds(1).build();
      daprClient.invokeMethod(APP_ID, "sleepOverGRPC", req.toByteArray(), HttpExtension.POST)
          .contextWrite(getReactorContext(openTelemetry))
          .block();
    }

    span.end();
    Validation.validate(spanName, "calllocal/tracingitgrpc-service/sleepovergrpc");
  }
}
