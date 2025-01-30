package io.dapr.workflows.saga;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

public class SagaOptionsTest {

  @Test
  public void testBuild() {
    SagaOptions.Builder builder = SagaOptions.newBuilder();
    builder.setParallelCompensation(true);
    builder.setMaxParallelThread(32);
    builder.setContinueWithError(false);
    SagaOptions option = builder.build();

    assertEquals(true, option.isParallelCompensation());
    assertEquals(32, option.getMaxParallelThread());
    assertEquals(false, option.isContinueWithError());
  }

  @Test
  public void testBuild_default() {
    SagaOptions.Builder builder = SagaOptions.newBuilder();
    SagaOptions option = builder.build();

    assertEquals(false, option.isParallelCompensation());
    assertEquals(16, option.getMaxParallelThread());
    assertEquals(true, option.isContinueWithError());
  }

  @Test
  public void testsetMaxParallelThread() {
    SagaOptions.Builder builder = SagaOptions.newBuilder();

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
