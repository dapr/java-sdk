package io.dapr.it.actors;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprClient;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.app.MyActor;
import io.dapr.it.actors.app.MyActorService;
import io.dapr.it.tracing.http.OpenTelemetryConfig;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import net.minidev.json.JSONArray;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActorTracingIT extends BaseIT {

    @Test
    public void testInvoke() throws Exception {
        var run = startDaprApp(
            ActorTracingIT.class.getSimpleName()+"Server",
            MyActorService.SUCCESS_MESSAGE,
            MyActorService.class,
            true,
            60000);
        var clientRun = startDaprApp(
            ActorTracingIT.class.getSimpleName()+"Client",
            60000);

        OpenTelemetry openTelemetry = createOpenTelemetry();
        Tracer tracer = openTelemetry.getTracer(OpenTelemetryConfig.TRACER_NAME);
        String spanName = UUID.randomUUID().toString();
        Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();

        try (DaprClient client = run.newDaprClientBuilder().build()) {
            MyActor myActor =
                new ActorProxyBuilder<>(
                    "MyActorTest",
                    MyActor.class,
                    clientRun.newActorClient()).build(new ActorId("123456"));
            try (Scope scope = span.makeCurrent()) {
                client.waitForSidecar(10000).block();
                myActor.say("hello world")
                    .contextWrite(getReactorContext(openTelemetry))
                    .block();
            }
        } finally {
            span.end();
        }

        Validation.validateGrandChild(
            spanName,
            "/dapr.proto.runtime.v1.dapr/invokeactor",
            "callactor/myactortest/say");
    }

    private static final class Validation {

        private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

        /**
         * JSON Path for main span Id.
         */
        public static final String JSONPATH_MAIN_SPAN_ID = "$..[?(@.name == \"%s\")]['id']";

        /**
         * JSON Path for child span.
         */
        public static final String JSONPATH_PARENT_SPAN_ID =
            "$..[?(@.parentId=='%s' && @.name=='%s')]['id']";

        public static void validateGrandChild(String grandParentSpanName, String parentSpanName, String grandChildSpanName) throws Exception {
            // Must wait for some time to make sure Zipkin receives all spans.
            Thread.sleep(10000);
            HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
            urlBuilder.scheme("http")
                .host("localhost")
                .port(9411)
                .addPathSegments("api/v2/traces")
                .addQueryParameter("limit", "100");
            Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build());
            requestBuilder.method("GET", null);

            Request request = requestBuilder.build();

            Response response = HTTP_CLIENT.newCall(request).execute();
            DocumentContext documentContext = JsonPath.parse(response.body().string());
            String grandParentSpanId = readOne(documentContext, String.format(JSONPATH_MAIN_SPAN_ID, grandParentSpanName)).toString();
            assertNotNull(grandParentSpanId);

            String parentSpanId = readOne(documentContext, String.format(JSONPATH_PARENT_SPAN_ID, grandParentSpanId,  parentSpanName))
                .toString();
            assertNotNull(parentSpanId);

            String grandChildSpanId = readOne(documentContext, String.format(JSONPATH_PARENT_SPAN_ID, parentSpanId,  grandChildSpanName))
                .toString();
            assertNotNull(grandChildSpanId);
        }

        private static Object readOne(DocumentContext documentContext, String path) {
            JSONArray arr = documentContext.read(path);
            assertTrue(arr.size() > 0);

            return arr.get(0);
        }

    }
}
