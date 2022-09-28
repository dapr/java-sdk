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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A request to bulk publish multiples events in a single call to Dapr.
 *
 * @param <T> Type parameter of the event.
 */
public class BulkPublishRequest<T> {

  /**
   * The name of the pubsub to publish to.
   */
  private String pubsubName;

  /**
   * The name of the topic to publish to.
   */
  private String topic;

  /**
   * The metadata for the request sent to the pubsub broker.
   * This is also used for setting common metadata for all entries in the request such as ttlInSeconds etc.
   */
  private Map<String, String> metadata;

  /**
   * The request entry objects that make up this request.
   */
  private List<BulkPublishRequestEntry<T>> entries;

  /**
   * Constructor for BulkPublishRequest.
   * @param pubsubName Name of the pubsub to publish to.
   * @param topic      Name of the topic to publish to.
   */
  public BulkPublishRequest(String pubsubName, String topic) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.entries = Collections.unmodifiableList(new ArrayList<>());
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
    this.metadata = metadata == null ? Collections.unmodifiableMap(new HashMap<>()) :
        Collections.unmodifiableMap(metadata);
    this.entries = entries == null ? Collections.unmodifiableList(new ArrayList<>()) :
        Collections.unmodifiableList(entries);
  }

  public String getPubsubName() {
    return pubsubName;
  }

  public String getTopic() {
    return topic;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public BulkPublishRequest<T> setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  public List<BulkPublishRequestEntry<T>> getEntries() {
    return entries;
  }

  public BulkPublishRequest<T> setEntries(List<BulkPublishRequestEntry<T>> entries) {
    this.entries = entries == null ? null : Collections.unmodifiableList(entries);
    return this;
  }
}
