package io.dapr.client;

class ClientRequest<T> {
  T body;
  String httpMethod;
  String httpUrl;
  String topic;

  ClientRequest(T body, String httpMethod, String httpUrl, String topic) {
    this.body = body;
    this.httpMethod = httpMethod;
    this.httpUrl = httpUrl;
    this.topic = topic;
  }

  public T getBody() {
    return body;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getHttpUrl() {
    return httpUrl;
  }

  public String getTopic() {
    return topic;
  }
}
