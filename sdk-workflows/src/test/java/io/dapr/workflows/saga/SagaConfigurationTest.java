package io.dapr.workflows.saga;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

public class SagaConfigurationTest {

  @Test
  public void testBuild() {
    SagaConfiguration.Builder builder = SagaConfiguration.newBuilder();
    builder.setParallelCompensation(true);
    builder.setMaxParallelThread(32);
    builder.setContinueWithError(false);
    SagaConfiguration config = builder.build();

    assertEquals(true, config.isParallelCompensation());
    assertEquals(32, config.getMaxParallelThread());
    assertEquals(false, config.isContinueWithError());
  }

  @Test
  public void testBuild_default() {
    SagaConfiguration.Builder builder = SagaConfiguration.newBuilder();
    SagaConfiguration config = builder.build();

    assertEquals(false, config.isParallelCompensation());
    assertEquals(16, config.getMaxParallelThread());
    assertEquals(true, config.isContinueWithError());
  }

  @Test
  public void testsetMaxParallelThread() {
    SagaConfiguration.Builder builder = SagaConfiguration.newBuilder();

    assertThrows(IllegalArgumentException.class, () -> {
      builder.setMaxParallelThread(0);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      builder.setMaxParallelThread(1);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      builder.setMaxParallelThread(-1);
    });
  }

}
