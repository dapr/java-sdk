package io.dapr.spring.boot.cloudconfig.configdata.types;

import org.springframework.util.StringUtils;

public class DaprCloudConfigType {
  /**
   * Get Type from String.
   * @param value type specified in schema
   * @param docType type of doc (if specified)
   * @return type enum
   */
  public static DaprCloudConfigType fromString(String value, String docType) {
    return "doc".equals(value)
        ? new DocType(StringUtils.hasText(docType) ? docType : "properties")
        : new ValueType();
  }
}
