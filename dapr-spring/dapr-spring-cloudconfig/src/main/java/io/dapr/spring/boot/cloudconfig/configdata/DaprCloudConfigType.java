package io.dapr.spring.boot.cloudconfig.configdata;

public enum DaprCloudConfigType {
  Doc,
  Value;

  /**
   * Get Type from String.
   * @param value type specified in schema
   * @return type enum
   */
  public static DaprCloudConfigType fromString(String value) {
    return "doc".equals(value)
        ? DaprCloudConfigType.Doc
        : DaprCloudConfigType.Value;
  }
}
