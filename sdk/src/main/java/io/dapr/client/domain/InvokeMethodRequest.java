/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

/**
 * A request to invoke a service.
 */
public class InvokeMethodRequest {

  private final String appId;

  private final String method;

  private Object body;

  private HttpExtension httpExtension;

  private String contentType;

  public InvokeMethodRequest(String appId, String method) {
    this.appId = appId;
    this.method = method;
  }

  public String getAppId() {
    return appId;
  }

  public String getMethod() {
    return method;
  }

  public Object getBody() {
    return body;
  }

  public InvokeMethodRequest setBody(Object body) {
    this.body = body;
    return this;
  }

  public HttpExtension getHttpExtension() {
    return httpExtension;
  }

  public InvokeMethodRequest setHttpExtension(HttpExtension httpExtension) {
    this.httpExtension = httpExtension;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public InvokeMethodRequest setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }
}
