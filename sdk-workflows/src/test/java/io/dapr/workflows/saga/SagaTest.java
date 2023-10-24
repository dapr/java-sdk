package io.dapr.workflows.saga;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SagaTest {

  @Test
  public void testSaga_IllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Saga(null);
    });
  }

  @Test
  public void testregisterCompensation_IllegalArgument() {
    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);

    assertThrows(IllegalArgumentException.class, () -> {
      saga.registerCompensation(null, "input", "output");
    });
  }

  @Test
  public void testCompensateInParallel() {
    MockActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockActivity.class.getName(), input1, "output");
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    saga.registerCompensation(MockActivity.class.getName(), input2, "output2");
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockActivity.class.getName(), input3, "output3");

    saga.compensate();

    assertEquals(3, MockActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception() {
    MockActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockActivity.class.getName(), input1, "output");
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    // set throw exception to true
    input2.setThrowException(true);
    saga.registerCompensation(MockActivity.class.getName(), input2, "output2");
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockActivity.class.getName(), input3, "output3");

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate();
    });
    // exception.printStackTrace();
    assertNotNull(exception.getCause());
    assertEquals(0, exception.getSuppressed().length);

    assertEquals(3, MockActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateInParallel_exception_Suppressed() {
    MockActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockActivity.class.getName(), input1, "output");
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    // set throw exception to true
    input2.setThrowException(true);
    saga.registerCompensation(MockActivity.class.getName(), input2, "output2");
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    // set throw exception to true
    input3.setThrowException(true);
    saga.registerCompensation(MockActivity.class.getName(), input3, "output3");

    SagaCompensationException exception = assertThrows(SagaCompensationException.class, () -> {
      saga.compensate();
    });
    // exception.printStackTrace();
    assertNotNull(exception.getCause());
    assertEquals(1, exception.getSuppressed().length);

    assertEquals(3, MockActivity.compensateOrder.size());
  }

  @Test
  public void testCompensateSequentially() {
    MockActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockActivity.class.getName(), input1, "output");
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    saga.registerCompensation(MockActivity.class.getName(), input2, "output2");
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockActivity.class.getName(), input3, "output3");

    saga.compensate();

    // the order should be 3 / 2 / 1
    assertEquals(Integer.valueOf(3), MockActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(2), MockActivity.compensateOrder.get(1));
    assertEquals(Integer.valueOf(1), MockActivity.compensateOrder.get(2));
  }

  @Test
  public void testCompensateSequentially_ContinueWithError() {
    MockActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockActivity.class.getName(), input1, "output");
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    // set throw exception to true
    input2.setThrowException(true);
    saga.registerCompensation(MockActivity.class.getName(), input2, "output2");
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockActivity.class.getName(), input3, "output3");

    saga.compensate();

    // the order should be 3 / 2 / 1
    assertEquals(Integer.valueOf(3), MockActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(2), MockActivity.compensateOrder.get(1));
    assertEquals(Integer.valueOf(1), MockActivity.compensateOrder.get(2));
  }

  @Test
  public void testCompensateSequentially_NotContinueWithError() {
    MockActivity.compensateOrder.clear();

    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(false)
        .setContinueWithError(false).build();
    Saga saga = new Saga(config);
    MockActivityInput input1 = new MockActivityInput();
    input1.setOrder(1);
    saga.registerCompensation(MockActivity.class.getName(), input1, "output");
    MockActivityInput input2 = new MockActivityInput();
    input2.setOrder(2);
    // set throw exception to true
    input2.setThrowException(true);
    saga.registerCompensation(MockActivity.class.getName(), input2, "output2");
    MockActivityInput input3 = new MockActivityInput();
    input3.setOrder(3);
    saga.registerCompensation(MockActivity.class.getName(), input3, "output3");

    assertThrows(SagaCompensationException.class, () -> {
      saga.compensate();
    });

    // the order should be 3 / 2
    assertEquals(Integer.valueOf(3), MockActivity.compensateOrder.get(0));
    assertEquals(Integer.valueOf(2), MockActivity.compensateOrder.get(1));
    assertEquals(2, MockActivity.compensateOrder.size());
  }

  public static class MockActivity implements CompensatableWorkflowActivity {

    private static List<Integer> compensateOrder = new ArrayList<>();

    @Override
    public void compensate(Object activityInput, Object activityOutput) {
      MockActivityInput input = (MockActivityInput) activityInput;
      compensateOrder.add(input.getOrder());

      if (input.isThrowException()) {
        throw new RuntimeException("compensate failed");
      }
    }
  }

  public static class MockActivityInput {
    private int order = 0;
    private boolean throwException;

    public int getOrder() {
      return order;
    }

    public void setOrder(int order) {
      this.order = order;
    }

    public boolean isThrowException() {
      return throwException;
    }

    public void setThrowException(boolean throwException) {
      this.throwException = throwException;
    }
  }

}
