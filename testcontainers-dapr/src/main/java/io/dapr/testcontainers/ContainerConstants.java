package io.dapr.testcontainers;

public interface ContainerConstants {
  String DAPR_VERSION = "1.15.3";
  String DAPR_PLACEMENT_IMAGE_TAG = "daprio/placement:" + DAPR_VERSION;
  String DAPR_RUNTIME_IMAGE_TAG = "daprio/daprd:" + DAPR_VERSION;
  String TOXIPROXY_IMAGE_TAG = "ghcr.io/shopify/toxiproxy:2.5.0";
}
