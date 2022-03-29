/*
 * Copyright 2021 The Dapr Authors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds a request to publish an event.
 * Deprecated in favor of @see{@link PublishEventRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0
 */
@Deprecated
public class PublishEventRequestBuilder {

  private final String pubsubName;

  private final String topic;

  private final Object data;

  private String contentType;

  private Map<String, String> metadata = new HashMap<>();

  /**
   * Instantiates a builder for a publish request.
   *
   * @param pubsubName Name of the Dapr PubSub.
   * @param topic      Topic name.
   * @param data       Data to be published.
   */
  public PublishEventRequestBuilder(String pubsubName, String topic, Object data) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.data = data;
  }

  public PublishEventRequestBuilder withContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public PublishEventRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public PublishEventRequest build() {
    PublishEventRequest request = new PublishEventRequest(pubsubName, topic, data);
    return request.setContentType(this.contentType)
        .setMetadata(this.metadata);
  }

}
