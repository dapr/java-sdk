/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.List;
import java.util.Map;

public class ExecuteStateTransactionRequest {

  /**
   * Name of the state store.
   */
  private final String stateStoreName;

  /**
   * Transactional operations list.
   */
  private final List<TransactionalStateOperation<?>> operations;

  /**
   * Metadata used for transactional operations.
   */
  private final Map<String, String> metadata;

  ExecuteStateTransactionRequest(String stateStoreName,
                                        List<TransactionalStateOperation<?>> operations,
                                        Map<String, String> metadata) {
    this.stateStoreName = stateStoreName;
    this.operations = operations;
    this.metadata = metadata;
  }

  public String getStateStoreName() {
    return stateStoreName;
  }

  public List<TransactionalStateOperation<?>> getOperations() {
    return operations;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
