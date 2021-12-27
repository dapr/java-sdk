/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.serializer.DaprObjectSerializer;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DaprPreviewClientBuilderTest {

  @Test
  public void build() {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    when(objectSerializer.getContentType()).thenReturn("application/json");
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprPreviewClientBuilder daprPreviewClientBuilder = new DaprPreviewClientBuilder();
    daprPreviewClientBuilder.withObjectSerializer(objectSerializer);
    daprPreviewClientBuilder.withStateSerializer(stateSerializer);
    DaprPreviewClient daprPreviewClient = daprPreviewClientBuilder.build();
    assertNotNull(daprPreviewClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noObjectSerializer() {
    new DaprPreviewClientBuilder().withObjectSerializer(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void blankContentTypeInObjectSerializer() {
    new DaprPreviewClientBuilder().withObjectSerializer(mock(DaprObjectSerializer.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void noStateSerializer() {
    new DaprPreviewClientBuilder().withStateSerializer(null);
  }
}
