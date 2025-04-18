package io.dapr.it.testcontainers;

public interface DaprContainerConstants {
  String DAPR_RUNTIME_VERSION = "1.15.4";
  String DAPR_RUNTIME_IMAGE_TAG = "daprio/daprd:" + DAPR_RUNTIME_VERSION;
  String DAPR_PLACEMENT_IMAGE_TAG = "daprio/placement:" + DAPR_RUNTIME_VERSION;
  String DAPR_SCHEDULER_IMAGE_TAG = "daprio/scheduler:" + DAPR_RUNTIME_VERSION;
}
