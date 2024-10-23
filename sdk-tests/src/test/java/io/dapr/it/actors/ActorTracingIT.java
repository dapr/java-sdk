package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprClient;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.app.MyActor;
import io.dapr.it.actors.app.MyActorService;
import io.dapr.it.tracing.Validation;
import io.dapr.it.tracing.http.OpenTelemetryConfig;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;

public class ActorTracingIT extends BaseIT {

    @Test
    public void testInvoke() throws Exception {
        var run = startDaprApp(
            ActorTracingIT.class.getSimpleName(),
            MyActorService.SUCCESS_MESSAGE,
            MyActorService.class,
            true,
            60000);
        var clientRun = startDaprApp(
            ActorTracingIT.class.getSimpleName()+"Client",
            60000);

        OpenTelemetry openTelemetry = createOpenTelemetry(OpenTelemetryConfig.SERVICE_NAME);
        Tracer tracer = openTelemetry.getTracer(OpenTelemetryConfig.TRACER_NAME);
        String spanName = UUID.randomUUID().toString();
        Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();

        try (DaprClient client = run.newDaprClientBuilder().build()) {
            client.waitForSidecar(10000).block();
            MyActor myActor =
                new ActorProxyBuilder<>(
                    "MyActorTest",
                    MyActor.class,
                    clientRun.newActorClient()).build(new ActorId("123456"));
            try (Scope scope = span.makeCurrent()) {
                myActor.say("hello world").contextWrite(getReactorContext(openTelemetry)).block();
            }
        }

        span.end();

        Validation.validate(spanName, "calllocal/tracingithttp-service/say");
    }

}
