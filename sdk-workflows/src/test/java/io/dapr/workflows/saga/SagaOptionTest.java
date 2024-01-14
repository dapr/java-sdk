package io.dapr.workflows.saga;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

public class SagaOptionTest {

  @Test
  public void testBuild() {
    SagaOption.Builder builder = SagaOption.newBuilder();
    builder.setParallelCompensation(true);
    builder.setMaxParallelThread(32);
    builder.setContinueWithError(false);
    SagaOption option = builder.build();

    assertEquals(true, option.isParallelCompensation());
    assertEquals(32, option.getMaxParallelThread());
    assertEquals(false, option.isContinueWithError());
  }

  @Test
  public void testBuild_default() {
    SagaOption.Builder builder = SagaOption.newBuilder();
    SagaOption option = builder.build();

    assertEquals(false, option.isParallelCompensation());
    assertEquals(16, option.getMaxParallelThread());
    assertEquals(true, option.isContinueWithError());
  }

  @Test
  public void testsetMaxParallelThread() {
    SagaOption.Builder builder = SagaOption.newBuilder();

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
