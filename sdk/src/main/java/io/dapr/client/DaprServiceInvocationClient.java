/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client;

import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service invocation API calls of {@link DaprClientImpl}: HTTP method invocation and the HTTP client for an app.
 */
final class DaprServiceInvocationClient {

  private final DaprHttp httpClient;

  private final DaprObjectSerializer objectSerializer;

  DaprServiceInvocationClient(DaprHttp httpClient, DaprObjectSerializer objectSerializer) {
    this.httpClient = httpClient;
    this.objectSerializer = objectSerializer;
  }

  @Deprecated
  <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    try {
      final String appId = invokeMethodRequest.getAppId();
      final String method = invokeMethodRequest.getMethod();
      final Object request = invokeMethodRequest.getBody();
      final HttpExtension httpExtension = invokeMethodRequest.getHttpExtension();
      final String contentType = invokeMethodRequest.getContentType();
      final Map<String, String> metadata = invokeMethodRequest.getMetadata();

      if (httpExtension == null) {
        throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
      }
      // If the httpExtension is not null, then the method will not be null based on checks in constructor
      final String httpMethod = httpExtension.getMethod().toString();
      if (appId == null || appId.trim().isEmpty()) {
        throw new IllegalArgumentException("App Id cannot be null or empty.");
      }
      if (method == null || method.trim().isEmpty()) {
        throw new IllegalArgumentException("Method name cannot be null or empty.");
      }


      String[] methodSegments = method.split("/");

      List<String> pathSegments = new ArrayList<>(Arrays.asList(DaprHttp.API_VERSION, "invoke", appId, "method"));
      pathSegments.addAll(Arrays.asList(methodSegments));

      final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      headers.putAll(httpExtension.getHeaders());
      if (metadata != null) {
        headers.putAll(metadata);
      }
      byte[] serializedRequestBody = objectSerializer.serialize(request);
      if (contentType != null && !contentType.isEmpty()) {
        headers.put(io.dapr.client.domain.Metadata.CONTENT_TYPE, contentType);
      } else {
        headers.putIfAbsent(io.dapr.client.domain.Metadata.CONTENT_TYPE, objectSerializer.getContentType());
      }
      Mono<DaprHttp.Response> response = Mono.deferContextual(
          context -> this.httpClient.invokeApi(httpMethod, pathSegments.toArray(new String[0]),
              httpExtension.getQueryParams(), serializedRequestBody, headers, context)
      );
      return response.flatMap(r -> getMonoForHttpResponse(type, r));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  DaprInvokeHttpClient invokeHttpClient(String appId) {
    if (appId == null || appId.trim().isEmpty()) {
      throw new IllegalArgumentException("App Id cannot be null or empty.");
    }
    URI invokeBase = this.httpClient.getBaseUri()
        .resolve("/" + DaprHttp.API_VERSION + "/invoke/" + appId + "/method/");
    return new DaprInvokeHttpClient(
        this.httpClient.getHttpClient(),
        invokeBase,
        this.httpClient.getDaprApiToken(),
        this.httpClient.getReadTimeout());
  }

  private <T> Mono<T> getMonoForHttpResponse(TypeRef<T> type, DaprHttp.Response r) {
    try {
      if (type == null) {
        return Mono.empty();
      }

      T object = objectSerializer.deserialize(r.getBody(), type);
      if (object == null) {
        return Mono.empty();
      }

      return Mono.just(object);
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }
}
