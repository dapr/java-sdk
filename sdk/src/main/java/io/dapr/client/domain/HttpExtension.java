/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.dapr.client.DaprHttp;
import okhttp3.HttpUrl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
  private DaprHttp.HttpMethods method;

  /**
   * HTTP query params.
   */
  private Map<String, List<String>> queryParams;

  /**
   * HTTP headers.
   */
  private Map<String, String> headers;

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
    if ((this.queryParams == null) || (this.queryParams.isEmpty())) {
      return "";
    }

    HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
    // Setting required values but we only need query params in the end.
    urlBuilder.scheme("http").host("localhost");
    Optional.ofNullable(this.queryParams).orElse(Collections.emptyMap()).entrySet().stream()
        .forEach(urlParameter ->
            Optional.ofNullable(urlParameter.getValue()).orElse(Collections.emptyList()).stream()
                .forEach(urlParameterValue ->
                    urlBuilder.addQueryParameter(urlParameter.getKey(), urlParameterValue)));
    return urlBuilder.build().encodedQuery();
  }
}
