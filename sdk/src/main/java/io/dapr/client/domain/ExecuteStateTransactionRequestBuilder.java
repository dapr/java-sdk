/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Deprecated in favor of @see{@link ExecuteStateTransactionRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0.
 */
@Deprecated
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
    return new ExecuteStateTransactionRequest(storeName)
        .setMetadata(metadata).setOperations(transactionalStates);
  }
}
