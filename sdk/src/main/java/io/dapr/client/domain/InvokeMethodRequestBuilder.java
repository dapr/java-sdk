/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

/**
 * Builds a request to invoke a service.
 */
public class InvokeMethodRequestBuilder {

  private final String appId;

  private final String method;

  private String contentType;

  private Object body;

  private HttpExtension httpExtension = HttpExtension.NONE;

  public InvokeMethodRequestBuilder(String appId, String method) {
    this.appId = appId;
    this.method = method;
  }

  public InvokeMethodRequestBuilder withContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public InvokeMethodRequestBuilder withBody(Object body) {
    this.body = body;
    return this;
  }

  public InvokeMethodRequestBuilder withHttpExtension(HttpExtension httpExtension) {
    this.httpExtension = httpExtension;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public InvokeMethodRequest build() {
    InvokeMethodRequest request = new InvokeMethodRequest();
    request.setAppId(this.appId);
    request.setContentType(contentType);
    request.setMethod(this.method);
    request.setBody(this.body);
    request.setHttpExtension(this.httpExtension);
    return request;
  }

}
