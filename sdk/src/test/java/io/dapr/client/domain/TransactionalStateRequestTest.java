/*
 * Copyright 2025 The Dapr Authors
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalStateRequestTest {


  @Test
  @DisplayName("Should create with non empty collections")
  void shouldCreateCorrectlyWithNonEmptyCollections() {

    State<String> users = new State<>("users");
    TransactionalStateOperation<String> operation =
        new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.DELETE, users);

    TransactionalStateRequest<String> request = new TransactionalStateRequest<>(
        List.of(operation), Map.of("application", "java-sdk")
    );

    assertThat(request.getMetadata()).hasSize(1);
    assertThat(request.getOperations()).hasSize(1);
  }

  @Test
  @DisplayName("Should create correctly with empty collections")
  void shouldCreateCorrectlyWithEmptyCollections() {
    TransactionalStateRequest<String> request = new TransactionalStateRequest<>(
        List.of(), Map.of()
    );
    assertThat(request.getMetadata()).isEmpty();
    assertThat(request.getOperations()).isEmpty();
  }

  @Test
  @DisplayName("Should create correctly with null value")
  void shouldCreateWhenArgumentIsNull() {
    TransactionalStateRequest<String> request = new TransactionalStateRequest<>(
        null, null
    );
    assertThat(request.getMetadata()).isNull();
    assertThat(request.getOperations()).isNull();
  }


}
