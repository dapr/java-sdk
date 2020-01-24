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
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(objectSerializer, stateSerializer);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildException() {
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder(null,null);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }


}