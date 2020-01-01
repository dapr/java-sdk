package io.dapr.client;

public class ClientRequestBuilder<T> {
  T body;
  String httpMethod;
  String httpUrl;
  String topic;

  public ClientRequestBuilder withBody(T body) {
    this.body = body;
    return this;
  }

  public ClientRequestBuilder withHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  public ClientRequestBuilder withHttpUrl(String httpUrl) {
    this.httpUrl = httpUrl;
    return this;
  }

  public ClientRequestBuilder withTopic(String topic) {
    this.topic = topic;
    return this;
  }

  public ClientRequest<T> build() {
    return new ClientRequest<>(body, httpMethod, httpUrl, topic);
  }
}
