/*
 * Copyright 2023 The Dapr Authors
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
public final class BulkPublishRequest<T> {

  /**
   * The name of the pubsub to publish to.
   */
  private final String pubsubName;

  /**
   * The name of the topic to publish to.
   */
  private final String topic;

  /**
   * The metadata for the request sent to the pubsub broker.
   * This is also used for setting common metadata for all entries in the request such as ttlInSeconds etc.
   */
  private Map<String, String> metadata;

  /**
   * The list of entry objects that make up this request.
   */
  private final List<BulkPublishEntry<T>> entries;

  /**
   * Constructor for BulkPublishRequest.
   * @param pubsubName Name of the pubsub to publish to.
   * @param topic      Name of the topic to publish to.
   * @param entries    List of {@link BulkPublishEntry} objects.
   */
  public BulkPublishRequest(String pubsubName, String topic, List<BulkPublishEntry<T>> entries) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.entries = entries == null ? Collections.unmodifiableList(new ArrayList<>()) :
        Collections.unmodifiableList(entries);
  }

  /**
   * Constructor for the BulkPublishRequest.
   *
   * @param pubsubName Name of the pubsub to publish to.
   * @param topic      Name of the topic to publish to.
   * @param entries    List of {@link BulkPublishEntry} objects.
   * @param metadata   Metadata for the request.
   */
  public BulkPublishRequest(String pubsubName, String topic, List<BulkPublishEntry<T>> entries,
                            Map<String, String> metadata) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.entries = entries == null ? List.of() : Collections.unmodifiableList(entries);
    this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
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

  public List<BulkPublishEntry<T>> getEntries() {
    return entries;
  }

}
