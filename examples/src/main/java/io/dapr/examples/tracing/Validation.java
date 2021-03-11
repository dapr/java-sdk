/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.examples.tracing;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Class used to verify that traces are present as expected.
 */
final class Validation {

  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

  public static final String JSONPATH_PROXY_ECHO_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.name=='calllocal/tracingdemoproxy/proxy_echo')]['id']";

  public static final String JSONPATH_ECHO_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.name=='calllocal/tracingdemo/echo')]['id']";

  public static final String JSONPATH_PROXY_SLEEP_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.duration > 1000000 && @.name=='calllocal/tracingdemoproxy/proxy_sleep')]['id']";

  public static final String JSONPATH_SLEEP_SPAN_ID =
      "$..[?(@.parentId=='%s' && @.duration > 1000000 && @.name=='calllocal/tracingdemo/sleep')]['id']";

  static void validate() throws Exception {
    // Must wait for some time to make sure Zipkin receives all spans.
    Thread.sleep(5000);
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
