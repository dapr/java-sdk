/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

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

  /**
   * Context to be passed on in the call.
   */
  private final Context context;

  ExecuteStateTransactionRequest(String stateStoreName,
                                        List<TransactionalStateOperation<?>> operations,
                                        Map<String, String> metadata,
                                        Context context) {
    this.stateStoreName = stateStoreName;
    this.operations = operations;
    this.metadata = metadata;
    this.context = context;
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

  public Context getContext() {
    return context;
  }
}
