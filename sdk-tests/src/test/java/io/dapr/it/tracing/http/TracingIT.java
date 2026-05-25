/*
 * Copyright 2025 The Dapr Authors
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

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.AppRun;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.it.containers.SharedTestInfra;
import io.dapr.it.tracing.Validation;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.TracingConfigurationSettings;
import io.dapr.testcontainers.ZipkinTracingConfigurationSettings;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;

@SuppressWarnings("deprecation")
public class TracingIT extends BaseContainerIT {

    private static final String APP_NAME = "tracing-http-it";

    private static DaprContainer dapr;
    private static AppRun app;
    private static String zipkinHostUrl;
    private static String zipkinTracesUrl;

    @BeforeAll
    public static void setup() throws Exception {
        // Start Zipkin first so we can wire its endpoint into both Dapr and the test's OpenTelemetry SDK.
        SharedTestInfra.zipkin();
        String zipkinHost = SharedTestInfra.zipkin().getHost();
        int zipkinPort = SharedTestInfra.zipkin().getMappedPort(9411);
        zipkinHostUrl = "http://" + zipkinHost + ":" + zipkinPort + "/api/v2/spans";
        zipkinTracesUrl = "http://" + zipkinHost + ":" + zipkinPort + "/api/v2/traces?limit=100";

        var pair = startAppAndAttach(
            APP_NAME,
            Service.class,
            AppRun.AppProtocol.HTTP,
            appPort -> {
                DaprContainer d = daprBuilder(APP_NAME)
                    .withAppPort(appPort)
                    .withAppChannelAddress("host.testcontainers.internal")
                    .withConfiguration(new Configuration(
                        "tracing",
                        new TracingConfigurationSettings(
                            "1",
                            true,
                            null,
                            new ZipkinTracingConfigurationSettings(SharedTestInfra.zipkinInternalEndpoint())
                        ),
                        null
                    ));
                d.start();
                return d;
            });
        dapr = pair.dapr();
        app = pair.app();

        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }

    @Test
    public void testInvoke() throws Exception {
        OpenTelemetry openTelemetry = createOpenTelemetry(OpenTelemetryConfig.SERVICE_NAME, zipkinHostUrl);
        Tracer tracer = openTelemetry.getTracer(OpenTelemetryConfig.TRACER_NAME);
        String spanName = UUID.randomUUID().toString();
        Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();

        try (DaprClient client = newDaprClient(dapr)) {
            client.waitForSidecar(10000).block();
            try (Scope scope = span.makeCurrent()) {
                client.invokeMethod(APP_NAME, "sleep", 1, HttpExtension.POST)
                    .contextWrite(getReactorContext(openTelemetry))
                    .block();
            }
        }

        span.end();

        Validation.validate(spanName, "calllocal/" + APP_NAME + "-service/sleep", zipkinTracesUrl);
    }
}
