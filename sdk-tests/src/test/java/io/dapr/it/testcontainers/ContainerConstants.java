package io.dapr.it.testcontainers;

import io.dapr.testcontainers.DaprContainerConstants;

public interface ContainerConstants {
  String DAPR_RUNTIME_IMAGE_TAG = DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
  // Built from dapr/dapr master. Needed by tests covering runtime features that no release
  // carries yet, where the pinned release image would silently ignore the new request fields.
  // The whole control plane moves together so the sidecar never talks to an older placement or
  // scheduler than it was built against.
  String DAPR_RUNTIME_EDGE_IMAGE_TAG = "daprio/daprd:edge";
  String DAPR_PLACEMENT_EDGE_IMAGE_TAG = "daprio/placement:edge";
  String DAPR_SCHEDULER_EDGE_IMAGE_TAG = "daprio/scheduler:edge";
  String DAPR_PLACEMENT_IMAGE_TAG = DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
  String DAPR_SCHEDULER_IMAGE_TAG = DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
  String TOXI_PROXY_IMAGE_TAG = "ghcr.io/shopify/toxiproxy:2.5.0";
  String JDK_17_TEMURIN_JAMMY = "eclipse-temurin:17-jdk-jammy";
}
