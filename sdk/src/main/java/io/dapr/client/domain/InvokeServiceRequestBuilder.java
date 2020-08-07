/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import io.grpc.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds a request to invoke a service.
 */
public class InvokeServiceRequestBuilder {

  private final String appId;

  private final String method;

  private Object body;

  private Map<String, String> metadata = new HashMap<>();

  private HttpExtension httpExtension = HttpExtension.NONE;

  private Context context;

  public InvokeServiceRequestBuilder(String appId, String method) {
    this.appId = appId;
    this.method = method;
  }

  public InvokeServiceRequestBuilder withBody(Object body) {
    this.body = body;
    return this;
  }

  public InvokeServiceRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public InvokeServiceRequestBuilder withHttpExtension(HttpExtension httpExtension) {
    this.httpExtension = httpExtension;
    return this;
  }

  public InvokeServiceRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public InvokeServiceRequest build() {
    InvokeServiceRequest request = new InvokeServiceRequest();
    request.setAppId(this.appId);
    request.setMethod(this.method);
    request.setBody(this.body);
    request.setHttpExtension(this.httpExtension);
    request.setMetadata(this.metadata);
    request.setContext(this.context);
    return request;
  }

}
