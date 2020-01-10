/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * A cloud event in Dapr.
 */
public final class CloudEventEnvelope {

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
   * Raw input payload.
   */
  private final byte[] data;

  /**
   * Instantiates a new input request.
   * @param id Identifier of the message being processed.
   * @param source Source for this event.
   * @param type Type of event.
   * @param specversion Version of the event spec.
   * @param datacontenttype Type of the payload.
   * @param data Payload.
   */
  public CloudEventEnvelope(
    String id,
    String source,
    String type,
    String specversion,
    String datacontenttype,
    byte[] data) {
    this.id = id;
    this.source = source;
    this.type = type;
    this.specversion = specversion;
    this.datacontenttype = datacontenttype;
    this.data = data;
  }

  /**
   * Gets the identifier of the message being processed.
   * @return Identifier of the message being processed.
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the source for this event.
   * @return Source for this event.
   */
  public String getSource() {
    return source;
  }

  /**
   * Gets the type of event.
   * @return Type of event.
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the version of the event spec.
   * @return Version of the event spec.
   */
  public String getSpecversion() {
    return specversion;
  }

  /**
   * Gets the type of the payload.
   * @return Type of the payload.
   */
  public String getDatacontenttype() {
    return datacontenttype;
  }

  /**
   * Gets the payload
   * @return Payload
   */
  public byte[] getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CloudEventEnvelope that = (CloudEventEnvelope) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(source, that.source) &&
      Objects.equals(type, that.type) &&
      Objects.equals(specversion, that.specversion) &&
      Objects.equals(datacontenttype, that.datacontenttype) &&
      Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, source, type, specversion, datacontenttype);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }
}
