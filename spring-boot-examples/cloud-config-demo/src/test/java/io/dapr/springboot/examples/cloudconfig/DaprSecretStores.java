package io.dapr.springboot.examples.cloudconfig;

public class DaprSecretStores {
  public static final String SINGLE_VALUED_SECRET = "{\n" +
      "  \"dapr.spring.demo-config-secret.singlevalue\": \"testvalue\",\n" +
      "  \"multivalue-properties\": \"dapr.spring.demo-config-secret.multivalue.v1=spring\\ndapr.spring.demo-config-secret.multivalue.v2=dapr\",\n" +
      "  \"multivalue-yaml\": \"dapr:\\n  spring:\\n    demo-config-secret:\\n      multivalue:\\n        v3: cloud\"\n" +
      "}";

  public static final String MULTI_VALUED_SECRET = "{\n" +
      "  \"value1\": {\n" +
      "    \"dapr\": {\n" +
      "      \"spring\": {\n" +
      "        \"demo-config-secret\": {\n" +
      "          \"multivalue\": {\n" +
      "            \"v4\": \"config\"\n" +
      "          }\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}";
}
