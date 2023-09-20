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

package io.dapr.examples.tracing;

import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.Status;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Class used to verify that traces are present as expected.
 */
final class Validation {

  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

  private static final RetryConfig RETRY_CONFIG = new RetryConfigBuilder()
      .withMaxNumberOfTries(5)
      .withFixedBackoff().withDelayBetweenTries(10, SECONDS)
      .retryOnAnyException()
      .build();

  public static final String JSONPATH_PROXY_ECHO_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.name=='calllocal/tracingdemoproxy/proxy_echo')]['id']";

  public static final String JSONPATH_ECHO_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.name=='calllocal/tracingdemo/echo')]['id']";

  public static final String JSONPATH_PROXY_SLEEP_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.duration > 1000000 && @.name=='calllocal/tracingdemoproxy/proxy_sleep')]['id']";

  public static final String JSONPATH_SLEEP_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.duration > 1000000 && @.name=='calllocal/tracingdemo/sleep')]['id']";

  static void validate() {
    Status<Void> result = new CallExecutorBuilder().config(RETRY_CONFIG).build().execute(() -> doValidate());
    if (!result.wasSuccessful()) {
      throw new RuntimeException(result.getLastExceptionThatCausedRetry());
    }
  }

  private static Void doValidate() throws Exception {
    System.out.println("Performing validation of tracing events ...");

    HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
    urlBuilder.scheme("http")
        .host("localhost")
        .port(9411);
    urlBuilder.addPathSegments("api/v2/traces");
    Request.Builder requestBuilder = new Request.Builder()
        .url(urlBuilder.build());
    requestBuilder.method("GET", null);

    Request request = requestBuilder.build();

    Response response = HTTP_CLIENT.newCall(request).execute();
    DocumentContext documentContext = JsonPath.parse(response.body().string());
    String mainSpanId = readOne(documentContext, "$..[?(@.name == \"example's main\")]['id']").toString();

    // Validate echo
    assertCount(documentContext,
        String.format(JSONPATH_PROXY_ECHO_SPAN_ID, mainSpanId),
        2);
    String proxyEchoSpanId = readOne(documentContext,
        String.format(JSONPATH_PROXY_ECHO_SPAN_ID, mainSpanId))
        .toString();
    String proxyEchoSpanId2 = readOne(documentContext,
        String.format(JSONPATH_PROXY_ECHO_SPAN_ID, proxyEchoSpanId))
        .toString();
    readOne(documentContext,
        String.format(JSONPATH_ECHO_SPAN_ID, proxyEchoSpanId2));

    // Validate sleep
    assertCount(documentContext,
        String.format(JSONPATH_PROXY_SLEEP_SPAN_ID, mainSpanId),
        2);
    String proxySleepSpanId = readOne(documentContext,
        String.format(JSONPATH_PROXY_SLEEP_SPAN_ID, mainSpanId))
        .toString();
    String proxySleepSpanId2 = readOne(documentContext,
        String.format(JSONPATH_PROXY_SLEEP_SPAN_ID, proxySleepSpanId))
        .toString();
    readOne(documentContext,
        String.format(JSONPATH_SLEEP_SPAN_ID, proxySleepSpanId2));
    System.out.println("Validation of tracing events has succeeded.");
    return null;
  }

  private static Object readOne(DocumentContext documentContext, String path) {
    JSONArray arr = documentContext.read(path);
    if (arr.size() == 0) {
      throw new RuntimeException("No record found for " + path);
    }

    return arr.get(0);
  }

  private static void assertCount(DocumentContext documentContext, String path, int expectedCount) {
    JSONArray arr = documentContext.read(path);
    if (arr.size() != expectedCount) {
      throw new RuntimeException(
          String.format("Unexpected count %d vs expected %d for %s", arr.size(), expectedCount, path));
    }
  }

}
