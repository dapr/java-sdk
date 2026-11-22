/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dapr.it.testcontainers.tracing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.fail;

/** Assertions for traces exported by the containerized tracing applications. */
public final class TraceAssertions {
  private TraceAssertions() {
  }

  /**
   * Waits for the named client span and its exact child in the same trace, with child duration over one second.
   * @param zipkin Zipkin endpoint reachable from the test JVM.
   * @param traceId Expected trace ID.
   * @param parentId Expected client span ID and child parent ID.
   * @param spanName Unique client span name.
   * @param childName Exact expected child span name.
   * @throws Exception If querying fails or the wait is interrupted.
   */
  public static void awaitTrace(URI zipkin, String traceId, String parentId, String spanName, String childName)
      throws Exception {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    ObjectMapper mapper = new ObjectMapper();
    URI traceUri = zipkin.resolve("/api/v2/trace/" + traceId);
    long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
    String lastObservation = "no response";
    String lastError = "none";
    while (System.nanoTime() < deadline) {
      HttpRequest request = HttpRequest.newBuilder(traceUri).timeout(Duration.ofSeconds(3)).GET().build();
      try {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        lastObservation = "HTTP " + response.statusCode() + ": " + response.body();
        if (response.statusCode() == 200) {
          JsonNode spans = mapper.readTree(response.body());
          if (!spans.isArray()) {
            throw new IllegalStateException("Unexpected Zipkin trace response: " + lastObservation);
          }
          boolean parentFound = false;
          boolean childFound = false;
          for (JsonNode candidate : spans) {
            if (!traceId.equals(candidate.path("traceId").asText())) {
              continue;
            }
            parentFound |= parentId.equals(candidate.path("id").asText())
                && spanName.equals(candidate.path("name").asText());
            childFound |= parentId.equals(candidate.path("parentId").asText())
                && childName.equals(candidate.path("name").asText())
                && candidate.path("duration").asLong() > 1_000_000;
          }
          if (parentFound && childFound) {
            System.out.printf("Trace validated: %s%n", response.body());
            return;
          }
        }
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Invalid Zipkin trace response: " + lastObservation, e);
      } catch (IOException e) {
        lastError = e.toString();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw e;
      }
    }
    fail("Timed out waiting for traceId=" + traceId + " client=" + spanName + " parentId=" + parentId
        + " child=" + childName + " duration > 1000000us; last observation: " + lastObservation
        + "; last request error: " + lastError);
  }
}
