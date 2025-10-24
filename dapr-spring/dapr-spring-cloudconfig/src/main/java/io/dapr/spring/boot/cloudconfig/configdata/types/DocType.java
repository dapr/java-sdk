package io.dapr.spring.boot.cloudconfig.configdata.types;

import org.springframework.util.StringUtils;

public class DocType extends DaprCloudConfigType {
  private final String docType;

  public DocType(String docType) {
    this.docType = StringUtils.hasText(docType) ? docType : "properties";
  }

  public String getDocType() {
    return docType;
  }

  public String getDocExtension() {
    String type = getDocType();
    return "." + StringUtils.trimLeadingCharacter(type, '.');
  }
}
