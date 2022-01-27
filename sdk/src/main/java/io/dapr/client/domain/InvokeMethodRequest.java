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

/**
 * A request to invoke a service.
 */
public class InvokeMethodRequest {

  private final String appId;

  private final String method;

  private Object body;

  private HttpExtension httpExtension;

  private String contentType;

  /**
   * Constructor for InvokeMethodRequest.
   *
   * @param appId   ID of the Dapr application
   * @param method  Name of the method to be invoked
   */
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
