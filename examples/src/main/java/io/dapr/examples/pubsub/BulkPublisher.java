/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.examples.pubsub;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkPublishResponseFailedEntry;
import io.dapr.examples.OpenTelemetryConfig;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

import java.util.ArrayList;
import java.util.List;

import static io.dapr.examples.OpenTelemetryConfig.getReactorContext;

/**
 * Message publisher.
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run the program:
 * dapr run --components-path ./components/pubsub --app-id bulk-publisher -- \
 * java -Ddapr.grpc.port="50010" -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.BulkPublisher
 */
public class BulkPublisher {

  private static final int NUM_MESSAGES = 10;

  private static final String TOPIC_NAME = "bulkpublishtesting";

  //The name of the pubsub
  private static final String PUBSUB_NAME = "messagebus";

  /**
   * main method.
   *
   * @param args incoming args
   * @throws Exception any exception
   */
  public static void main(String[] args) throws Exception {
    OpenTelemetry openTelemetry = OpenTelemetryConfig.createOpenTelemetry();
    Tracer tracer = openTelemetry.getTracer(BulkPublisher.class.getCanonicalName());
    Span span = tracer.spanBuilder("Bulk Publisher's Main").setSpanKind(Span.Kind.CLIENT).startSpan();
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      DaprClient c = (DaprClient) client;
      c.waitForSidecar(10000);
      try (Scope scope = span.makeCurrent()) {
        System.out.println("Using preview client...");
        List<String> messages = new ArrayList<>();
        System.out.println("Constructing the list of messages to publish");
        for (int i = 0; i < NUM_MESSAGES; i++) {
          String message = String.format("This is message #%d", i);
          messages.add(message);
          System.out.println("Going to publish message : " + message);
        }
        BulkPublishResponse<?> res = client.publishEvents(PUBSUB_NAME, TOPIC_NAME, "text/plain", messages)
            .contextWrite(getReactorContext()).block();
        System.out.println("Published the set of messages in a single call to Dapr");
        if (res != null) {
          if (res.getFailedEntries().size() > 0) {
            // Ideally this condition will not happen in examples
            System.out.println("Some events failed to be published");
            for (BulkPublishResponseFailedEntry<?> entry : res.getFailedEntries()) {
              System.out.println("EntryId : " + entry.getEntry().getEntryId()
                  + " Error message : " + entry.getErrorMessage());
            }
          }
        } else {
          throw new Exception("null response from dapr");
        }
      }
      // Close the span.

      span.end();
      // Allow plenty of time for Dapr to export all relevant spans to the tracing infra.
      Thread.sleep(10000);
      // Shutdown the OpenTelemetry tracer.
      OpenTelemetrySdk.getGlobalTracerManagement().shutdown();

      System.out.println("Done");
    }
  }
}

