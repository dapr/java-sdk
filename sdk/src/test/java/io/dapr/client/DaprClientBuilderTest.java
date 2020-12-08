/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  @Test(expected = DaprException.class)
  public void noObjectSerializer() {
    new DaprClientBuilder().withObjectSerializer(null);
  }

  @Test(expected = DaprException.class)
  public void blankContentTypeInObjectSerializer() {
    new DaprClientBuilder().withObjectSerializer(mock(DaprObjectSerializer.class));
  }

  @Test(expected = DaprException.class)
  public void noStateSerializer() {
    new DaprClientBuilder().withStateSerializer(null);
  }

}