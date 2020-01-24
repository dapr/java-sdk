package io.dapr.client;

import org.junit.Test;
import java.time.Duration;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class DaprHttpBuilderTest {

  @Test
  public void withReadTimeout() throws Exception {
    DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();
    Duration duration = mock(Duration.class);
    daprHttpBuilder.build();
    DaprHttpBuilder dapr = daprHttpBuilder.withReadTimeout(duration);
    assertNotNull(dapr);
  }

}