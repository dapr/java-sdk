package io.dapr.client;

import javax.net.ssl.SSLSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class MockHttpResponse implements HttpResponse<Object> {

  private final byte[] body;
  private final int statusCode;

  public MockHttpResponse(int statusCode) {
    this.body = null;
    this.statusCode = statusCode;
  }

  public MockHttpResponse(byte[] body, int statusCode) {
    this.body = body;
    this.statusCode = statusCode;
  }

  @Override
  public int statusCode() {
    return statusCode;
  }

  @Override
  public HttpRequest request() {
    return null;
  }

  @Override
  public Optional<HttpResponse<Object>> previousResponse() {
    return Optional.empty();
  }

  @Override
  public HttpHeaders headers() {
    return HttpHeaders.of(Collections.emptyMap(), (a, b) -> true);
  }

  @Override
  public byte[] body() {
    return body;
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    return null;
  }

  @Override
  public HttpClient.Version version() {
    return null;
  }
}
