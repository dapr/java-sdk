/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.testcontainers.wait.strategy.metadata;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  @DisplayName("Metadata should return empty list when actors is null")
  void metadataShouldReturnEmptyListWhenActorsIsNull() {
    Metadata metadata = new Metadata();

    assertNotNull(metadata.getActors());
    assertTrue(metadata.getActors().isEmpty());
  }

  @Test
  @DisplayName("Metadata should return empty list when components is null")
  void metadataShouldReturnEmptyListWhenComponentsIsNull() {
    Metadata metadata = new Metadata();

    assertNotNull(metadata.getComponents());
    assertTrue(metadata.getComponents().isEmpty());
  }

  @Test
  @DisplayName("Metadata should return empty list when subscriptions is null")
  void metadataShouldReturnEmptyListWhenSubscriptionsIsNull() {
    Metadata metadata = new Metadata();

    assertNotNull(metadata.getSubscriptions());
    assertTrue(metadata.getSubscriptions().isEmpty());
  }

  @Test
  @DisplayName("Metadata should store and retrieve all fields correctly")
  void metadataShouldStoreAndRetrieveAllFields() {
    Metadata metadata = new Metadata();
    metadata.setId("test-app");
    metadata.setRuntimeVersion("1.14.0");
    metadata.setEnabledFeatures(Arrays.asList("feature1", "feature2"));

    Actor actor = new Actor();
    actor.setType("MyActor");
    metadata.setActors(Collections.singletonList(actor));

    Component component = new Component();
    component.setName("statestore");
    metadata.setComponents(Collections.singletonList(component));

    Subscription subscription = new Subscription();
    subscription.setTopic("orders");
    metadata.setSubscriptions(Collections.singletonList(subscription));

    assertEquals("test-app", metadata.getId());
    assertEquals("1.14.0", metadata.getRuntimeVersion());
    assertEquals(2, metadata.getEnabledFeatures().size());
    assertEquals(1, metadata.getActors().size());
    assertEquals(1, metadata.getComponents().size());
    assertEquals(1, metadata.getSubscriptions().size());
  }

  @Test
  @DisplayName("Actor should store and retrieve all fields correctly")
  void actorShouldStoreAndRetrieveAllFields() {
    Actor actor = new Actor();
    actor.setType("OrderActor");
    actor.setCount(5);

    assertEquals("OrderActor", actor.getType());
    assertEquals(5, actor.getCount());
  }

  @Test
  @DisplayName("Component should store and retrieve all fields correctly")
  void componentShouldStoreAndRetrieveAllFields() {
    Component component = new Component();
    component.setName("statestore");
    component.setType("state.redis");
    component.setVersion("v1");
    component.setCapabilities(Arrays.asList("ETAG", "TRANSACTIONAL"));

    assertEquals("statestore", component.getName());
    assertEquals("state.redis", component.getType());
    assertEquals("v1", component.getVersion());
    assertEquals(2, component.getCapabilities().size());
    assertTrue(component.getCapabilities().contains("ETAG"));
  }

  @Test
  @DisplayName("Subscription should store and retrieve all fields including rules")
  void subscriptionShouldStoreAndRetrieveAllFields() {
    Subscription subscription = new Subscription();
    subscription.setPubsubname("pubsub");
    subscription.setTopic("orders");
    subscription.setDeadLetterTopic("orders-dlq");
    subscription.setType("declarative");

    Map<String, String> meta = new HashMap<>();
    meta.put("key", "value");
    subscription.setMetadata(meta);

    Subscription.Rule rule = new Subscription.Rule();
    rule.setMatch("event.type == 'order'");
    rule.setPath("/orders");
    subscription.setRules(Collections.singletonList(rule));

    assertEquals("pubsub", subscription.getPubsubname());
    assertEquals("orders", subscription.getTopic());
    assertEquals("orders-dlq", subscription.getDeadLetterTopic());
    assertEquals("declarative", subscription.getType());
    assertEquals("value", subscription.getMetadata().get("key"));
    assertEquals(1, subscription.getRules().size());
    assertEquals("event.type == 'order'", subscription.getRules().get(0).getMatch());
    assertEquals("/orders", subscription.getRules().get(0).getPath());
  }

  @Test
  @DisplayName("Should deserialize complete Dapr metadata JSON response")
  void shouldDeserializeMetadataFromJson() throws Exception {
    String json = "{"
        + "\"id\": \"my-app\","
        + "\"runtimeVersion\": \"1.14.0\","
        + "\"enabledFeatures\": [\"ServiceInvocationStreaming\"],"
        + "\"actors\": [{\"type\": \"OrderActor\", \"count\": 3}],"
        + "\"components\": [{\"name\": \"statestore\", \"type\": \"state.redis\", \"version\": \"v1\", \"capabilities\": [\"ETAG\"]}],"
        + "\"subscriptions\": [{"
        + "  \"pubsubname\": \"pubsub\","
        + "  \"topic\": \"orders\","
        + "  \"deadLetterTopic\": \"orders-dlq\","
        + "  \"type\": \"programmatic\","
        + "  \"rules\": [{\"match\": \"\", \"path\": \"/orders\"}]"
        + "}]"
        + "}";

    Metadata metadata = OBJECT_MAPPER.readValue(json, Metadata.class);

    assertEquals("my-app", metadata.getId());
    assertEquals("1.14.0", metadata.getRuntimeVersion());
    assertEquals(1, metadata.getEnabledFeatures().size());

    assertEquals(1, metadata.getActors().size());
    assertEquals("OrderActor", metadata.getActors().get(0).getType());
    assertEquals(3, metadata.getActors().get(0).getCount());

    assertEquals(1, metadata.getComponents().size());
    assertEquals("statestore", metadata.getComponents().get(0).getName());
    assertEquals("state.redis", metadata.getComponents().get(0).getType());

    assertEquals(1, metadata.getSubscriptions().size());
    assertEquals("pubsub", metadata.getSubscriptions().get(0).getPubsubname());
    assertEquals("orders", metadata.getSubscriptions().get(0).getTopic());
    assertEquals(1, metadata.getSubscriptions().get(0).getRules().size());
  }

  @Test
  @DisplayName("Should ignore unknown fields when deserializing JSON")
  void shouldDeserializeMetadataWithUnknownFields() throws Exception {
    String json = "{"
        + "\"id\": \"my-app\","
        + "\"unknownField\": \"should be ignored\","
        + "\"anotherUnknown\": {\"nested\": true}"
        + "}";

    Metadata metadata = OBJECT_MAPPER.readValue(json, Metadata.class);

    assertEquals("my-app", metadata.getId());
    assertTrue(metadata.getActors().isEmpty());
  }
}
