/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.dapr.client.DaprHttp;

import java.util.Collections;
import java.util.HashMap;
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
  public static final HttpExtension NONE = new HttpExtension(DaprHttp.HttpMethods.NONE, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#GET} Verb with empty queryString.
   */
  public static final HttpExtension GET = new HttpExtension(DaprHttp.HttpMethods.GET, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#PUT} Verb with empty queryString.
   */
  public static final HttpExtension PUT = new HttpExtension(DaprHttp.HttpMethods.PUT, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#POST} Verb with empty queryString.
   */
  public static final HttpExtension POST = new HttpExtension(DaprHttp.HttpMethods.POST, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#DELETE} Verb with empty queryString.
   */
  public static final HttpExtension DELETE = new HttpExtension(DaprHttp.HttpMethods.DELETE, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#HEAD} Verb with empty queryString.
   */
  public static final HttpExtension HEAD = new HttpExtension(DaprHttp.HttpMethods.HEAD, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#CONNECT} Verb with empty queryString.
   */
  public static final HttpExtension CONNECT = new HttpExtension(DaprHttp.HttpMethods.CONNECT, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#OPTIONS} Verb with empty queryString.
   */
  public static final HttpExtension OPTIONS = new HttpExtension(DaprHttp.HttpMethods.OPTIONS, new HashMap<>());
  /**
   * Convenience HttpExtension object for the {@link DaprHttp.HttpMethods#TRACE} Verb with empty queryString.
   */
  public static final HttpExtension TRACE = new HttpExtension(DaprHttp.HttpMethods.TRACE, new HashMap<>());

  /**
   * HTTP verb.
   */
  private DaprHttp.HttpMethods method;

  /**
   * HTTP querystring.
   */
  private Map<String, String> queryString;

  /**
   * Construct a HttpExtension object.
   * @param method      Required value denoting the HttpMethod.
   * @param queryString Non-null map value for the queryString for a HTTP listener.
   * @see io.dapr.client.DaprHttp.HttpMethods for supported methods.
   * @throws IllegalArgumentException on null method or queryString.
   */
  public HttpExtension(DaprHttp.HttpMethods method, Map<String, String> queryString) {
    if (method == null) {
      throw new IllegalArgumentException("HttpExtension method cannot be null");
    } else if (queryString == null) {
      throw new IllegalArgumentException("HttpExtension queryString map cannot be null");
    }
    this.method = method;
    this.queryString = Collections.unmodifiableMap(queryString);
  }

  public DaprHttp.HttpMethods getMethod() {
    return method;
  }

  public Map<String, String> getQueryString() {
    return queryString;
  }
}
