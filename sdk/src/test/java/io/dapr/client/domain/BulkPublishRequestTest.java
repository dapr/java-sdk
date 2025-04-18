/*
 * Copyright 2023 The Dapr Authors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BulkPublishRequestTest {

  @Test
  public void testSetMetadata() {
    BulkPublishRequest<String> request = new BulkPublishRequest<>("testPubsub", "testTopic", Collections.emptyList());
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    Map<String, String> initial = request.getMetadata();
    request.setMetadata(metadata);

    assertThat(request.getMetadata()).isNotSameAs(initial);
  }

  @Test
  @DisplayName("Should create a BulkPublishRequest with empty list when entries is null")
  public void shouldCreateWithEmptyListWhenEntriesIsNull() {
    BulkPublishRequest<String> request = new BulkPublishRequest<>("testPubsub", "testTopic", null);
    List<BulkPublishEntry<String>> entries = request.getEntries();
    assertThat(entries).isNotNull();
  }
}
