package io.dapr.it.spring.cloudconfig;

public class DaprConfigurationStores {
  public static final String YAML_CONFIG = "dapr:\n" +
      "    spring:\n" +
      "        demo-config-config:\n" +
      "            multivalue:\n" +
      "                v3: cloud";
}
