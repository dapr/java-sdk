/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.tracing;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Class used to verify that traces are present as expected.
 *
 * Checks for the main span and then checks for its child span for `sleep` API call.
 */
public final class Validation {

  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

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
    Thread.sleep(5000);
    HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
    urlBuilder.scheme("http")
        .host("localhost")
        .port(9411)
        .addPathSegments("api/v2/traces");
    Request.Builder requestBuilder = new Request.Builder()
        .url(urlBuilder.build());
    requestBuilder.method("GET", null);

    Request request = requestBuilder.build();

    Response response = HTTP_CLIENT.newCall(request).execute();
    DocumentContext documentContext = JsonPath.parse(response.body().string());
    String mainSpanId = readOne(documentContext, String.format(JSONPATH_MAIN_SPAN_ID, spanName)).toString();
    assertNotNull(mainSpanId);

    String sleepSpanId = readOne(documentContext, String.format(JSONPATH_SLEEP_SPAN_ID, mainSpanId,  sleepSpanName))
        .toString();
    assertNotNull(sleepSpanId);
  }

  private static Object readOne(DocumentContext documentContext, String path) {
    JSONArray arr = documentContext.read(path);
    assertTrue(arr.size() > 0);

    return arr.get(0);
  }

}
