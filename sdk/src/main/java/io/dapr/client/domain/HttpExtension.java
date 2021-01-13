/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.dapr.client.DaprHttp;

import java.util.Collections;
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
   * HTTP querystring.
   */
  private Map<String, String> queryString;

  /**
   * HTTP headers.
   */
  private Map<String, String> headers;

  /**
   * Construct a HttpExtension object.
   * @param method      Required value denoting the HttpMethod.
   * @param queryString map for the queryString the HTTP call.
   * @param headers     map to set HTTP headers.
   * @see io.dapr.client.DaprHttp.HttpMethods for supported methods.
   * @throws IllegalArgumentException on null method or queryString.
   */
  public HttpExtension(DaprHttp.HttpMethods method, Map<String, String> queryString, Map<String, String> headers) {
    if (method == null) {
      throw new IllegalArgumentException("HttpExtension method cannot be null");
    }

    this.method = method;
    this.queryString = Collections.unmodifiableMap(queryString == null ? Collections.EMPTY_MAP : queryString);
    this.headers = Collections.unmodifiableMap(headers == null ? Collections.EMPTY_MAP : headers);
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

  public Map<String, String> getQueryString() {
    return queryString;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}
