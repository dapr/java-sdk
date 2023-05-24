package io.dapr.it.tracing.grpc;

import io.dapr.client.DaprApiProtocol;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.tracing.Validation;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TracingIT extends BaseIT {

    /**
     * Parameters for this test.
     * Param #1: useGrpc.
     * @return Collection of parameter tuples.
     */
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    @Parameter
    public boolean useGrpc;

    @Before
    public void init() throws Exception {
        daprRun = startDaprApp(
          TracingIT.class.getSimpleName(),
          Service.SUCCESS_MESSAGE,
          Service.class,
          DaprApiProtocol.GRPC,  // appProtocol
          60000);

        if (this.useGrpc) {
            daprRun.switchToGRPC();
        } else {
            daprRun.switchToHTTP();
        }

        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }

    @Test
    public void testInvoke() throws Exception {
        final OpenTelemetry openTelemetry = createOpenTelemetry("service over grpc");
        final Tracer tracer = openTelemetry.getTracer("grpc integration test tracer");

        final String spanName = UUID.randomUUID().toString();
        Span span = tracer.spanBuilder(spanName).setSpanKind(Span.Kind.CLIENT).startSpan();

        try (DaprClient client = new DaprClientBuilder().build()) {
            try (Scope scope = span.makeCurrent()) {
                SleepRequest req = SleepRequest.newBuilder().setSeconds(1).build();
                client.invokeMethod(daprRun.getAppName(), "sleepOverGRPC", req.toByteArray(), HttpExtension.POST)
                    .contextWrite(getReactorContext())
                    .block();
            }
        }
        span.end();
        OpenTelemetrySdk.getGlobalTracerManagement().shutdown();

        Validation.validate(spanName, "calllocal/tracingit-service/sleepovergrpc");
    }
}
