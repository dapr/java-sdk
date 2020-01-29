package io.dapr.client;

import io.dapr.serializer.DaprObjectSerializer;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DaprClientBuilderTest {

  @Test
  public void build() {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder();
    daprClientBuilder.withObjectSerializer(objectSerializer);
    daprClientBuilder.withStateSerializer(stateSerializer);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noObjectSerializer() {
    new DaprClientBuilder().withObjectSerializer(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noStateSerializer() {
    new DaprClientBuilder().withStateSerializer(null);
  }

}