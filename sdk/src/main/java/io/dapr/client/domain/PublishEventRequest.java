/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Map;

/**
 * A request to publish an event.
 */
public class PublishEventRequest {

  private String pubsubName;

  private String topic;

  private Object data;

  private String contentType;

  private Map<String, String> metadata;

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

  public String getContentType() {
    return this.contentType;
  }

  void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
