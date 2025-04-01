/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.feign;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.Util;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This module directs Feign's requests to <a href="https://dapr.io/">Dapr</a>, which is a microservice framework. Ex.
 *
 * <p>Currently, Dapr supports two ways to invoke operations, which is <b>invokeBinding (Output Binding)</b> and
 * <b>invokeMethod</b>, so this Client supports two schemas: <b>http://binding.xxx</b> or <b>http://method.xxx</b><br>
 * You don't have to mind why there is a http schema at start, it's just a trick to hack Spring Boot Openfeign.
 *
 * <p>For invokeMethod, there are two types of information in the url, which is very similar to an HTTP URL, except that
 * the host in the HTTP URL is converted to appId, and the path (excluding "/") is converted to methodName.<br>
 * For example, if you have a method which appid is "myApp", and methodName is "getAll", then the url of this request is
 * "http://method.myApp/getAll".<br>
 * You can also set HTTP Headers if you like, the Client will handle them.<br>
 * <b>Currently only HTTP invokes are supported, but I think we will support grpc invokes in the future, may be the url
 * will be "http://method_grpc.myApp/getAll" or something.</b>
 *
 * <p>For invokeBinding, there are also two types of information in the url, the host is the bindingName, and the path
 * is the operation.<br>
 * <b>Note: different bindings support different operations, so you have to check
 * <a href="https://docs.dapr.io/zh-hans/reference/components-reference/supported-bindings/">the documentation of
 * Dapr</a> for that.</b><br>
 * For example, if you have a binding which bindingName is "myBinding", and the operation supported is "create", and
 * then the url of this request is "http://binding.myBinding/create"<br>
 * You can put some metadata into the Header of your Feign Request, the Client will handle them.
 *
 * <p>As for response, the result code is always 200 OK, and if client have met any error, it will throw an IOException
 * for that.<br>
 * Currently, we have no method to gain metadata from server as Dapr Client doesn't have methods to do that, so headers
 * will be blank. If Accept header has set in request, a fake Content-Type header will be created in response, and it
 * will be the first value of Accept header.
 *
 * <pre>
 * MyAppData response = Feign.builder().client(new DaprFeignClient()).target(MyAppData.class,
 * "http://binding.myBinding/create");
 * </pre>
 */
public class DaprInvokeFeignClient implements Client {

  private static final String DOT = "\\.";

  private final int retry;
  private final int timeout;

  private final DaprClient daprClient;

  /**
   * Default Client creation with no arguments.
   *
   * <p>
   * retry 5 tries with 2000ms waiting and default DaprClient.
   * </p>
   */
  public DaprInvokeFeignClient() {
    daprClient = new DaprClientBuilder().build();
    retry = 2000;
    timeout = 3;
  }

  /**
   * Client creation with DaprClient Specified.
   *
   * <p>
   * retry 5 tries with 2000ms waiting.
   * </p>
   *
   * @param daprClient client sepcified
   */
  public DaprInvokeFeignClient(DaprClient daprClient) {
    this.daprClient = daprClient;
    retry = 2000;
    timeout = 5;
  }

  /**
   * Client creation with DaprClient, wait time, retry time Specified.
   *
   * @param daprClient client sepcified
   * @param timeout   wait time (ms)
   * @param retry  retry times
   */
  public DaprInvokeFeignClient(DaprClient daprClient, int timeout, int retry) {
    this.daprClient = daprClient;
    this.timeout = timeout;
    this.retry = retry;
  }

  @Override
  public Response execute(Request request, Request.Options options) throws IOException {
    URI uri;
    try {
      uri = new URI(request.url());
    } catch (URISyntaxException e) {
      throw new IOException("URL '" + request.url() + "' couldn't be parsed into a URI", e);
    }

    String schemaAndHost = uri.getHost();

    String[] splitSchemaAndHost = schemaAndHost.split(DOT);
    String schema;

    if (splitSchemaAndHost.length >= 2) {
      schema = splitSchemaAndHost[0];
    } else {
      throw new IOException("Host '" + schemaAndHost + "' is not supported");
    }

    Mono<byte[]> result = null;

    switch (schema) {
      case "method":
        result = getResultFromInvokeHTTPMethodRequest(uri, request);
        break;
      case "binding":
        result = getResultFromInvokeBindingRequest(uri, request);
        break;
      default:
        throw new IOException("Schema '" + schema + "' is not supported");
    }

    Map<String, Collection<String>> headerMap = new HashMap<>();

    if (request.headers().containsKey("Accept")) {
      headerMap.put("Content-Type", List.of(request.headers().get("Accept")
          .stream().findFirst()
          .orElseThrow(() -> new IOException("Accept header can not be null"))));
    }

    return Response.builder()
        .status(200)
        .reason("OK")
        .request(request)
        .headers(headerMap)
        .body(toResponseBody(result, options))
        .build();
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private Mono<byte[]> getResultFromInvokeHTTPMethodRequest(URI uri, Request request) throws IOException {
    String[] splitSchemaAndHost = uri.getHost().split(DOT);

    List<String> hostList = new ArrayList<>(List.of(splitSchemaAndHost));
    hostList.remove(0);

    InvokeMethodRequest invokeMethodRequest = null;
    try {
      invokeMethodRequest = toInvokeMethodHTTPRequest(new URI("method",
          uri.getUserInfo(),
          String.join(".", hostList),
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          uri.getFragment()), request);
    } catch (URISyntaxException e) {
      throw new IOException("URL '" + request.url() + "' couldn't be parsed into a URI", e);
    }

    return daprClient.invokeMethod(invokeMethodRequest, TypeRef.BYTE_ARRAY);

  }

  private Mono<byte[]> getResultFromInvokeBindingRequest(URI uri, Request request) throws IOException {
    String[] splitSchemaAndHost = uri.getHost().split(DOT);

    List<String> hostList = new ArrayList<>(List.of(splitSchemaAndHost));
    hostList.remove(0);

    InvokeBindingRequest invokeBindingRequest = null;
    try {
      invokeBindingRequest = toInvokeBindingRequest(new URI("binding",
          uri.getUserInfo(),
          String.join(".", hostList),
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          uri.getFragment()), request);
    } catch (URISyntaxException e) {
      throw new IOException("URL '" + request.url() + "' couldn't be parsed into a URI", e);
    }

    return daprClient.invokeBinding(invokeBindingRequest, TypeRef.BYTE_ARRAY);
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private InvokeMethodRequest toInvokeMethodHTTPRequest(URI uri, Request request) throws IOException {

    String path = uri.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 2);
    }

    InvokeMethodRequest invokeMethodRequest = new InvokeMethodRequest(uri.getHost(), path);
    invokeMethodRequest.setMetadata(toHeader(request.headers()));

    if (request.body() != null) {
      invokeMethodRequest.setBody(request.body());
    }

    invokeMethodRequest.setContentType(getContentType(request));
    invokeMethodRequest.setHttpExtension(toHttpExtension(request.httpMethod().name().toLowerCase()));

    return invokeMethodRequest;
  }

  private HttpExtension toHttpExtension(String method) throws IOException {
    switch (method) {
      case "none":
        return HttpExtension.NONE;
      case "put":
        return HttpExtension.PUT;
      case "post":
        return HttpExtension.POST;
      case "delete":
        return HttpExtension.DELETE;
      case "head":
        return HttpExtension.HEAD;
      case "connect":
        return HttpExtension.CONNECT;
      case "options":
        return HttpExtension.OPTIONS;
      case "trace":
        return HttpExtension.TRACE;
      case "get":
        return HttpExtension.GET;
      default:
        throw new IOException("Method '" + method + "' is not supported");
    }
  }

  private String getContentType(Request request) {
    String contentType = null;
    for (Map.Entry<String, Collection<String>> entry : request.headers().entrySet()) {
      if (entry.getKey().equalsIgnoreCase("Content-Type")) {
        Collection<String> values = entry.getValue();
        if (values != null && !values.isEmpty()) {
          contentType = values.iterator().next();
          break;
        }
      }
    }
    return contentType;
  }

  private InvokeBindingRequest toInvokeBindingRequest(URI uri, Request request) throws IOException {
    String path = uri.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 2);
    }

    if (path.split("/").length > 1) {
      throw new IOException("Binding Operation '" + path + "' is not reconigzed");
    }

    InvokeBindingRequest invokeBindingRequest = new InvokeBindingRequest(uri.getHost(), path);
    invokeBindingRequest.setMetadata(toHeader(request.headers()));

    if (request.body() != null) {
      invokeBindingRequest.setData(request.body());
    }

    return invokeBindingRequest;
  }

  private Map<String, String> toHeader(Map<String, Collection<String>> header) {
    Map<String, String> headerMap = new HashMap<>();

    // request headers
    for (Map.Entry<String, Collection<String>> headerEntry : header.entrySet()) {
      String headerName = headerEntry.getKey();

      if (headerName.equalsIgnoreCase(Util.CONTENT_LENGTH)) {
        continue;
      }

      for (String headerValue : headerEntry.getValue()) {
        headerMap.put(headerName, headerValue);
      }
    }

    return headerMap;
  }

  private Response.Body toResponseBody(Mono<byte[]> response, Request.Options options) throws IOException {
    byte[] result = new byte[0];

    for (int count = 0; count < retry; count++) {
      try {
        result = response.block(Duration.of(timeout,
            TimeUnit.MILLISECONDS.toChronoUnit()));

        if (result == null) {
          result = new byte[0];
        }

        break;
      } catch (RuntimeException e) {
        if (retry == count + 1) {
          throw new IOException("Can not get Response", e);
        }
      }
    }


    byte[] finalResult = result;
    return new Response.Body() {
      @Override
      public Integer length() {
        return finalResult.length;
      }

      @Override
      public boolean isRepeatable() {
        return true;
      }

      @Override
      public InputStream asInputStream() throws IOException {
        return new ByteArrayInputStream(finalResult);
      }

      @SuppressWarnings("deprecation")
      @Override
      public Reader asReader() throws IOException {
        return new InputStreamReader(asInputStream(), UTF_8);
      }

      @Override
      public Reader asReader(Charset charset) throws IOException {
        return new InputStreamReader(asInputStream(), charset);
      }

      @Override
      public void close() throws IOException {

      }
    };
  }

}
