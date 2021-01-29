/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

import java.util.Map;

/**
 * A request to publish an event.
 */
public class PublishEventRequest {

  private String pubsubName;

  private String topic;

  private Object data;

  private Map<String, String> metadata;

  private Context context;

  public String getPubsubName() {
    return pubsubName;
  }

  void setPubsubName(String pubsubName) {
    this.pubsubName = pubsubName;
  }

  public String getTopic() {
    return topic;
  }

  void setTopic(String topic) {
    this.topic = topic;
  }

  public Object getData() {
    return data;
  }

  void setData(Object data) {
    this.data = data;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public Context getContext() {
    return context;
  }

  void setContext(Context context) {
    this.context = context;
  }
}
