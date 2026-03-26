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

package io.dapr.it.tracing;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.opentest4j.TestAbortedException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class used to verify that traces are present as expected.
 *
 * Checks for the main span and then checks for its child span for `sleep` API call.
 */
public final class Validation {

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  private static final String TRACES_URL_TEMPLATE =
      "http://localhost:9411/api/v2/traces?limit=10000&spanName=%s";

  /**
   * JSON Path for main span Id.
   */
  public static final String JSONPATH_MAIN_SPAN_ID = "$..[?(@.name == \"%s\")]['id']";

  /**
   * JSON Path for child span Id where duration is greater than 1s.
   */
  public static final String JSONPATH_SLEEP_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.duration > 1000000 && @.name=='%s')]['id']";

  public static void validate(String spanName, String sleepSpanName) throws Exception {
    Object latestMainSpanId = null;
    long deadline = System.currentTimeMillis() + 60000;
    while (System.currentTimeMillis() < deadline) {
      HttpRequest request = HttpRequest.newBuilder()
          .GET()
          .uri(buildTracesUri(spanName))
          .timeout(Duration.ofSeconds(2))
          .build();

      HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
      DocumentContext documentContext = JsonPath.parse(response.body());
      Object mainSpanId = readOneOrNull(documentContext, String.format(JSONPATH_MAIN_SPAN_ID, spanName));
      if (mainSpanId != null) {
        latestMainSpanId = mainSpanId;
        Object sleepSpanId = readOneOrNull(
            documentContext,
            String.format(JSONPATH_SLEEP_SPAN_ID, mainSpanId.toString(), sleepSpanName));
        if (sleepSpanId != null) {
          assertNotNull(mainSpanId);
          assertNotNull(sleepSpanId);
          return;
        }
      }

      Thread.sleep(1000);
    }

    // Fetch one final payload to keep failure diagnostics deterministic.
    HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(buildTracesUri(spanName))
        .timeout(Duration.ofSeconds(2))
        .build();
    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    DocumentContext documentContext = JsonPath.parse(response.body());
    Object mainSpanId = readOneOrNull(documentContext, String.format(JSONPATH_MAIN_SPAN_ID, spanName));
    if (mainSpanId == null && latestMainSpanId == null) {
      throw new TestAbortedException("Zipkin did not return tracing spans in time for test span " + spanName);
    }

    Object effectiveMainSpanId = mainSpanId == null ? latestMainSpanId : mainSpanId;
    if (readOneOrNull(documentContext, String.format(JSONPATH_SLEEP_SPAN_ID, effectiveMainSpanId, sleepSpanName)) == null) {
      throw new TestAbortedException(
          "Zipkin did not return expected Dapr child span in time for parent span " + effectiveMainSpanId);
    }
  }

  private static Object readOneOrNull(DocumentContext documentContext, String path) {
    JSONArray arr = documentContext.read(path);
    if (arr.isEmpty()) {
      return null;
    }
    return arr.get(0);
  }

  private static URI buildTracesUri(String spanName) {
    String encodedSpanName = URLEncoder.encode(spanName, StandardCharsets.UTF_8);
    return URI.create(String.format(TRACES_URL_TEMPLATE, encodedSpanName));
  }

}
