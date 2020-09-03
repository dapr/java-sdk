/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.grpc.Context;

import java.util.Map;

/**
 * A request to invoke a service.
 */
public class InvokeServiceRequest {

  private String appId;

  private String method;

  private Object body;

  private Map<String, String> metadata;

  private HttpExtension httpExtension;

  private Context context;

  private String contentType;

  public String getAppId() {
    return appId;
  }

  void setAppId(String appId) {
    this.appId = appId;
  }

  public String getMethod() {
    return method;
  }

  void setMethod(String method) {
    this.method = method;
  }

  public Object getBody() {
    return body;
  }

  void setBody(Object body) {
    this.body = body;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public HttpExtension getHttpExtension() {
    return httpExtension;
  }

  void setHttpExtension(HttpExtension httpExtension) {
    this.httpExtension = httpExtension;
  }

  public Context getContext() {
    return context;
  }

  void setContext(Context context) {
    this.context = context;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
