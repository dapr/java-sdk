/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * A cloud event in Dapr.
 */
public final class CloudEvent {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation.
   */
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  /**
   * Identifier of the message being processed.
   */
  private final String id;

  /**
   * Event's source.
   */
  private final String source;

  /**
   * Envelope type.
   */
  private final String type;

  /**
   * Version of the specification.
   */
  private final String specversion;

  /**
   * Type of the data's content.
   */
  private final String datacontenttype;

  /**
   * Cloud event specs says data can be a JSON object or string.
   */
  private final String data;

  /**
   * Instantiates a new input request.
   *
   * @param id              Identifier of the message being processed.
   * @param source          Source for this event.
   * @param type            Type of event.
   * @param specversion     Version of the event spec.
   * @param datacontenttype Type of the payload.
   * @param data            Payload.
   */
  public CloudEvent(
      String id,
      String source,
      String type,
      String specversion,
      String datacontenttype,
      String data) {
    this.id = id;
    this.source = source;
    this.type = type;
    this.specversion = specversion;
    this.datacontenttype = datacontenttype;
    this.data = data;
  }

  /**
   * Gets the identifier of the message being processed.
   *
   * @return Identifier of the message being processed.
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the source for this event.
   *
   * @return Source for this event.
   */
  public String getSource() {
    return source;
  }

  /**
   * Gets the type of event.
   *
   * @return Type of event.
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the version of the event spec.
   *
   * @return Version of the event spec.
   */
  public String getSpecversion() {
    return specversion;
  }

  /**
   * Gets the type of the payload.
   *
   * @return Type of the payload.
   */
  public String getDatacontenttype() {
    return datacontenttype;
  }

  /**
   * Gets the payload.
   *
   * @return Payload
   */
  public String getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CloudEvent that = (CloudEvent) o;
    return Objects.equals(id, that.id)
        && Objects.equals(source, that.source)
        && Objects.equals(type, that.type)
        && Objects.equals(specversion, that.specversion)
        && Objects.equals(datacontenttype, that.datacontenttype)
        && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source, type, specversion, datacontenttype, data);
  }

  /**
   * Deserialized a message topic from Dapr.
   *
   * @param payload Payload sent from Dapr.
   * @return Message (can be null if input is null)
   * @throws IOException If cannot parse.
   */
  public static CloudEvent deserialize(byte[] payload) throws IOException {
    if (payload == null) {
      return null;
    }

    JsonNode node = OBJECT_MAPPER.readTree(payload);

    if (node == null) {
      return null;
    }

    String id = null;
    if (node.has("id") && !node.get("id").isNull()) {
      id = node.get("id").asText();
    }

    String source = null;
    if (node.has("source") && !node.get("source").isNull()) {
      source = node.get("source").asText();
    }

    String type = null;
    if (node.has("type") && !node.get("type").isNull()) {
      type = node.get("type").asText();
    }

    String specversion = null;
    if (node.has("specversion") && !node.get("specversion").isNull()) {
      specversion = node.get("specversion").asText();
    }

    String datacontenttype = null;
    if (node.has("datacontenttype") && !node.get("datacontenttype").isNull()) {
      datacontenttype = node.get("datacontenttype").asText();
    }

    String data = null;
    if (node.has("data") && !node.get("data").isNull()) {
      JsonNode dataNode = node.get("data");
      if (dataNode.isTextual()) {
        data = dataNode.textValue();
      } else {
        data = node.get("data").toString();
      }
    }

    return new CloudEvent(id, source, type, specversion, datacontenttype, data);
  }
}
