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

package io.dapr.client.domain;

import io.dapr.client.DaprHttp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP Extension class.
 * This class is only needed if the app you are calling is listening on HTTP.
 * It contains properties that represent data that may be populated for an HTTP receiver.
 */
public final class HttpExtension {
  /**
   * Convenience HttpExtension object for {@link io.dapr.client.DaprHttp.HttpMethods#NONE} with empty queryString.
   */
  public static final HttpExtension NONE = new HttpExtension(DaprHttp.HttpMethods.NONE);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#GET} Verb with empty queryString.
   */
  public static final HttpExtension GET = new HttpExtension(DaprHttp.HttpMethods.GET);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#PUT} Verb with empty queryString.
   */
  public static final HttpExtension PUT = new HttpExtension(DaprHttp.HttpMethods.PUT);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#POST} Verb with empty queryString.
   */
  public static final HttpExtension POST = new HttpExtension(DaprHttp.HttpMethods.POST);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#DELETE} Verb with empty queryString.
   */
  public static final HttpExtension DELETE = new HttpExtension(DaprHttp.HttpMethods.DELETE);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#PATCH} Verb with empty queryString.
   */
  public static final HttpExtension PATCH = new HttpExtension(DaprHttp.HttpMethods.PATCH);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#HEAD} Verb with empty queryString.
   */
  public static final HttpExtension HEAD = new HttpExtension(DaprHttp.HttpMethods.HEAD);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#CONNECT} Verb with empty queryString.
   */
  public static final HttpExtension CONNECT = new HttpExtension(DaprHttp.HttpMethods.CONNECT);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#OPTIONS} Verb with empty queryString.
   */
  public static final HttpExtension OPTIONS = new HttpExtension(DaprHttp.HttpMethods.OPTIONS);
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#TRACE} Verb with empty queryString.
   */
  public static final HttpExtension TRACE = new HttpExtension(DaprHttp.HttpMethods.TRACE);

  /**
   * HTTP verb.
   */
  private final DaprHttp.HttpMethods method;

  /**
   * HTTP query params.
   */
  private final Map<String, List<String>> queryParams;

  /**
   * HTTP headers.
   */
  private final Map<String, String> headers;

  /**
   * Construct a HttpExtension object.
   * @param method      Required value denoting the HttpMethod.
   * @param queryParams map for the query parameters the HTTP call.
   * @param headers     map to set HTTP headers.
   * @see io.dapr.client.DaprHttp.HttpMethods for supported methods.
   * @throws IllegalArgumentException on null method or queryString.
   */
  public HttpExtension(DaprHttp.HttpMethods method,
      Map<String, List<String>> queryParams,
      Map<String, String> headers) {
    if (method == null) {
      throw new IllegalArgumentException("HttpExtension method cannot be null");
    }

    this.method = method;
    this.queryParams = Collections.unmodifiableMap(queryParams == null ? Collections.emptyMap() : queryParams);
    this.headers = Collections.unmodifiableMap(headers == null ? Collections.emptyMap() : headers);
  }

  /**
   * Construct a HttpExtension object.
   * @param method      Required value denoting the HttpMethod.
   * @see io.dapr.client.DaprHttp.HttpMethods for supported methods.
   * @throws IllegalArgumentException on null method or queryString.
   */
  public HttpExtension(DaprHttp.HttpMethods method) {
    this(method, null, null);
  }

  public DaprHttp.HttpMethods getMethod() {
    return method;
  }

  public Map<String, List<String>> getQueryParams() {
    return queryParams;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Encodes the query string for the HTTP request.
   * @return Encoded HTTP query string.
   */
  public String encodeQueryString() {
    if (queryParams == null || queryParams.isEmpty()) {
      return "";
    }

    StringBuilder queryBuilder = new StringBuilder();

    for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
      String key = entry.getKey();
      List<String> values = entry.getValue();

      for (String value : values) {
        if (queryBuilder.length() > 0) {
          queryBuilder.append("&");
        }

        queryBuilder.append(encodeQueryParam(key, value)); // Encode key and value
      }
    }

    return queryBuilder.toString();
  }

  private static String encodeQueryParam(String key, String value) {
    return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
