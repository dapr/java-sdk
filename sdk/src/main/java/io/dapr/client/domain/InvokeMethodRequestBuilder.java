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
 * Builds a request to invoke a service.
 * Deprecated in favor of @see{@link InvokeMethodRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0
 */
@Deprecated
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
   *
   * @return Request object.
   */
  public InvokeMethodRequest build() {
    InvokeMethodRequest request = new InvokeMethodRequest(appId, method);
    return request.setBody(this.body)
        .setContentType(contentType)
        .setHttpExtension(this.httpExtension);
  }

}
