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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BulkPublishEntryTest {

  @Test
  @DisplayName("Should create an empty metadata when metadata argument is null")
  public void shouldCreateWithEmptyMetadataWhenMetadataIsNull() {
    BulkPublishEntry<String> entry = new BulkPublishEntry<>(
        "entryId", "event", "contentType", null
    );
    assertThat(entry.getMetadata()).isEmpty();
  }

  @Test
  @DisplayName("Should create with non null metadata")
  public void shouldCreateWithMetadata() {
    BulkPublishEntry<String> entry = new BulkPublishEntry<>(
        "entryId", "event", "application/json", Map.of(
        "repo", "dapr/java-sdk"
    ));
    assertThat(entry.getMetadata()).hasSize(1);
  }

}
