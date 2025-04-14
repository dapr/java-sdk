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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class used to verify that traces are present as expected.
 *
 * Checks for the main span and then checks for its child span for `sleep` API call.
 */
public final class Validation {

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .build();

  private static final String TRACES_URL = "http://localhost:9411/api/v2/traces?limit=100";

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
    // Must wait for some time to make sure Zipkin receives all spans.
    Thread.sleep(10000);

    HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(TRACES_URL))
        .build();

    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    DocumentContext documentContext = JsonPath.parse(response.body());
    String mainSpanId = readOne(documentContext, String.format(JSONPATH_MAIN_SPAN_ID, spanName)).toString();

    assertNotNull(mainSpanId);

    String sleepSpanId = readOne(documentContext, String.format(JSONPATH_SLEEP_SPAN_ID, mainSpanId,  sleepSpanName))
        .toString();

    assertNotNull(sleepSpanId);
  }

  private static Object readOne(DocumentContext documentContext, String path) {
    JSONArray arr = documentContext.read(path);

    assertFalse(arr.isEmpty());

    return arr.get(0);
  }

}
