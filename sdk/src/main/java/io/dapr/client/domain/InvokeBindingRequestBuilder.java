/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds a request to publish an event.
 */
public class InvokeBindingRequestBuilder {

  private final String name;

  private final String operation;

  private Object data;

  private Map<String, String> metadata = new HashMap<>();

  public InvokeBindingRequestBuilder(String name, String operation) {
    this.name = name;
    this.operation = operation;
  }

  public InvokeBindingRequestBuilder withData(Object data) {
    this.data = data;
    return this;
  }

  public InvokeBindingRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public InvokeBindingRequest build() {
    InvokeBindingRequest request = new InvokeBindingRequest();
    request.setName(this.name);
    request.setOperation(this.operation);
    request.setData(this.data);
    request.setMetadata(this.metadata);
    return request;
  }

}
