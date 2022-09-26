/*
 * Copyright 2022 The Dapr Authors
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

import java.util.List;
import java.util.Map;

/**
 * A request to bulk publish event.
 *
 * @param <T> Type parameter of the event.
 */
public class BulkPublishRequest<T> {

  private String pubsubName;
  private String topic;
  private Map<String, String> metadata;
  private List<BulkPublishRequestEntry<T>> entries;

  /**
   * Default constructor for BulkPublishRequest.
   */
  public BulkPublishRequest() {
  }

  /**
   * Constructor for the BulkPublishRequest.
   *
   * @param pubsubName Name of the pubsub to publish to.
   * @param topic      Name of the topic to publish to.
   * @param metadata   Metadata for the request.
   * @param entries    List of BulkPublishRequestEntry objects.
   */
  public BulkPublishRequest(String pubsubName, String topic, Map<String, String> metadata,
                            List<BulkPublishRequestEntry<T>> entries) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.metadata = metadata;
    this.entries = entries;
  }

  public String getPubsubName() {
    return pubsubName;
  }

  public void setPubsubName(String pubsubName) {
    this.pubsubName = pubsubName;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public List<BulkPublishRequestEntry<T>> getEntries() {
    return entries;
  }

  public void setEntries(List<BulkPublishRequestEntry<T>> entries) {
    this.entries = entries;
  }
}
