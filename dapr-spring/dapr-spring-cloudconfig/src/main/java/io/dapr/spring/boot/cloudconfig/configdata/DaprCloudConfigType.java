package io.dapr.spring.boot.cloudconfig.configdata;

public enum DaprCloudConfigType {
  DocProperties,
  DocYaml,
  Value;

  /**
   * Get Type from String.
   * @param value type specified in schema
   * @param docType type of doc (if specified)
   * @return type enum
   */
  public static DaprCloudConfigType fromString(String value, String docType) {
    return "doc".equals(value)
        ? ("yaml".equals(docType) ? DaprCloudConfigType.DocYaml : DaprCloudConfigType.DocProperties)
        : DaprCloudConfigType.Value;
  }
}
