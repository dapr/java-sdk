package io.dapr.it.testcontainers;

import io.dapr.testcontainers.DaprContainerConstants;

public interface ContainerConstants {
  String DAPR_RUNTIME_IMAGE_TAG = DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
  String DAPR_PLACEMENT_IMAGE_TAG = DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
  String DAPR_SCHEDULER_IMAGE_TAG = DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
  String TOXI_PROXY_IMAGE_TAG = "ghcr.io/shopify/toxiproxy:2.5.0";
  String JDK_17_TEMURIN_JAMMY = "eclipse-temurin:17-jdk-jammy";
}
