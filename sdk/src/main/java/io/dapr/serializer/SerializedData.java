package io.dapr.serializer;

/**
 * Represents a serialized data, with its content type.
 */
public class SerializedData {

  public static final SerializedData NULL = new SerializedData(null, null);

  /**
   * The data has been be serialized.
   */
  private final byte[] data;

  /**
   * The content type of the serialized data.
   */
  private final String contentType;

  public SerializedData(byte[] data, String contentType) {
    this.data = data;
    this.contentType = contentType;
  }

  public byte[] getData() {
    return data;
  }

  public String getContentType() {
    return contentType;
  }
}
