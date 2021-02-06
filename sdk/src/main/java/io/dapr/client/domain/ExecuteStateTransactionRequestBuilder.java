/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ExecuteStateTransactionRequestBuilder {
  private final String storeName;
  private List<TransactionalStateOperation<?>> transactionalStates;
  private Map<String, String> metadata;

  public ExecuteStateTransactionRequestBuilder(String storeName) {
    this.storeName = storeName;
  }

  public ExecuteStateTransactionRequestBuilder withTransactionalStates(
      TransactionalStateOperation<?>... transactionalStates) {
    this.transactionalStates = Collections.unmodifiableList(Arrays.asList(transactionalStates));
    return this;
  }

  public ExecuteStateTransactionRequestBuilder withTransactionalStates(
      List<TransactionalStateOperation<?>> transactionalStates) {
    this.transactionalStates = transactionalStates == null ? null : Collections.unmodifiableList(transactionalStates);
    return this;
  }

  public ExecuteStateTransactionRequestBuilder withMetadata(Map<String, String> metadata) {
    this.metadata = Collections.unmodifiableMap(metadata);
    return this;
  }

  public ExecuteStateTransactionRequest build() {
    return new ExecuteStateTransactionRequest(storeName, transactionalStates, metadata);
  }
}
