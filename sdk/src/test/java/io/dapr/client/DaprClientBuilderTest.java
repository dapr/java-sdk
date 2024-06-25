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

package io.dapr.client;

import io.dapr.serializer.DaprObjectSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DaprClientBuilderTest {

  @Test
  public void build() {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    when(objectSerializer.getContentType()).thenReturn("application/json");
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder();
    daprClientBuilder.withObjectSerializer(objectSerializer);
    daprClientBuilder.withStateSerializer(stateSerializer);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }

  @Test
  public void noObjectSerializer() {
    assertThrows(IllegalArgumentException.class, () -> { new DaprClientBuilder().withObjectSerializer(null);});
  }

  @Test
  public void blankContentTypeInObjectSerializer() {
    assertThrows(IllegalArgumentException.class, () -> { new DaprClientBuilder().withObjectSerializer(mock(DaprObjectSerializer.class));});
  }

  @Test
  public void noStateSerializer() {
      assertThrows(IllegalArgumentException.class, () -> { new DaprClientBuilder().withStateSerializer(null);});
  }

}
