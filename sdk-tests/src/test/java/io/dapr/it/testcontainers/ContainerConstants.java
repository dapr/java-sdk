package io.dapr.it.testcontainers;

public interface ContainerConstants {
  String TOXIPROXY_IMAGE_TAG = "ghcr.io/shopify/toxiproxy:2.5.0";
  String DAPR_RUNTIME_VERSION = "1.15.3";
  String DAPR_IMAGE_TAG = "daprio/daprd:" + DAPR_RUNTIME_VERSION;
  String DAPR_PLACEMENT_IMAGE_TAG = "daprio/placement:" + DAPR_RUNTIME_VERSION;
  String DAPR_SCHEDULER_IMAGE_TAG = "daprio/scheduler:" + DAPR_RUNTIME_VERSION;
}
