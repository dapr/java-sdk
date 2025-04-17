package io.dapr.testcontainers;

public interface DaprContainerConstants {
  String DAPR_VERSION = "1.15.4";
  String DAPR_PLACEMENT_IMAGE_TAG = "daprio/placement:" + DAPR_VERSION;
  String DAPR_SCHEDULER_IMAGE_TAG = "daprio/scheduler:" + DAPR_VERSION;
  String DAPR_RUNTIME_IMAGE_TAG = "daprio/daprd:" + DAPR_VERSION;
}
