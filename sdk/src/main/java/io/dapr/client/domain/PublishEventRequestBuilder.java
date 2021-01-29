/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds a request to publish an event.
 */
public class PublishEventRequestBuilder {

  private final String pubsubName;

  private final String topic;

  private final Object data;

  private Map<String, String> metadata = new HashMap<>();

  private Context context;

  /**
   * Instantiates a builder for a publish request.
   * @param pubsubName Name of the Dapr PubSub.
   * @param topic Topic name.
   * @param data Data to be published.
   */
  public PublishEventRequestBuilder(String pubsubName, String topic, Object data) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.data = data;
  }

  public PublishEventRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public PublishEventRequestBuilder withContext(Context context) {
    this.context = context;
    return this;
  }

  /**
   * Builds a request object.
   * @return Request object.
   */
  public PublishEventRequest build() {
    PublishEventRequest request = new PublishEventRequest();
    request.setPubsubName(this.pubsubName);
    request.setTopic(this.topic);
    request.setData(this.data);
    request.setMetadata(this.metadata);
    request.setContext(this.context);
    return request;
  }

}
