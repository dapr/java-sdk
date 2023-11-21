package io.dapr.workflows.saga;

import org.junit.Test;

import com.microsoft.durabletask.TaskActivityContext;

import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;

import static org.junit.jupiter.api.Assertions.assertEquals;;

public class SagaIntegrationTest {

  @Test
  public void testSaga_CompensateSequentially() {
    int runCount = 100;
    int succeedCount = 0;
    int compensateCount = 0;

    for (int i = 0; i < runCount; i++) {
      boolean isSuccueed = doExecuteWorkflowWithSaga(false);
      if (isSuccueed) {
        succeedCount++;
      } else {
        compensateCount++;
      }
    }

    System.out.println("Run workflow with saga " + runCount + " times: succeed " + succeedCount
        + " times, failed and compensated " + compensateCount + " times");
  }

  @Test
  public void testSaga_compensateInParallel() {
    int runCount = 100;
    int succeedCount = 0;
    int compensateCount = 0;

    for (int i = 0; i < runCount; i++) {
      boolean isSuccueed = doExecuteWorkflowWithSaga(true);
      if (isSuccueed) {
        succeedCount++;
      } else {
        compensateCount++;
      }
    }

    System.out.println("Run workflow with saga " + runCount + " times: succeed " + succeedCount
        + " times, failed and compensated " + compensateCount + " times");
  }

  private boolean doExecuteWorkflowWithSaga(boolean parallelCompensation) {
    SagaOption config = SagaOption.newBuilder()
        .setParallelCompensation(parallelCompensation)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);
    boolean workflowSuccess = false;

    // reset count to zero
    count = 0;
    Integer addInput = 100;
    Integer subtractInput = 20;
    Integer multiplyInput = 10;
    Integer divideInput = 5;

    try {
      // step1: add activity
      callActiviry(AddActivity.class.getName(), addInput, String.class);
      saga.registerCompensation(AddActivity.class.getName(), addInput);

      // step2: subtract activity
      callActiviry(SubtractActivity.class.getName(), subtractInput, String.class);
      saga.registerCompensation(SubtractActivity.class.getName(), subtractInput);

      if (parallelCompensation) {
        // only add/subtract activities support parallel compensation
        // so in step3 and step4 we repeat add/subtract activities

        // step3: add activity again
        callActiviry(AddActivity.class.getName(), addInput, String.class);
        saga.registerCompensation(AddActivity.class.getName(), addInput);

        // step4: substract activity again
        callActiviry(SubtractActivity.class.getName(), subtractInput, String.class);
        saga.registerCompensation(SubtractActivity.class.getName(), subtractInput);
      } else {
        // step3: multiply activity
        callActiviry(MultiplyActivity.class.getName(), multiplyInput, String.class);
        saga.registerCompensation(MultiplyActivity.class.getName(), multiplyInput);

        // step4: divide activity
        callActiviry(DivideActivity.class.getName(), divideInput, String.class);
        saga.registerCompensation(DivideActivity.class.getName(), divideInput);
      }

      randomFail();

      workflowSuccess = true;
    } catch (Exception e) {
      saga.compensate();
    }

    if (workflowSuccess) {
      int expectResult = 0;
      if (parallelCompensation) {
        expectResult = 0 + addInput - subtractInput + addInput - subtractInput;
      } else {
        expectResult = (0 + addInput - subtractInput) * multiplyInput / divideInput;
      }
      assertEquals(expectResult, count);
    } else {
      assertEquals(0, count);
    }

    return workflowSuccess;
  }

  private static void randomFail() {
    int randomInt = (int) (Math.random() * 100);
    // if randomInt mod 10 is 0, then throw exception
    if (randomInt % 10 == 0) {
      throw new RuntimeException("random fail");
    }
  }

  // mock to call activity in dapr workflow
  private <V> V callActiviry(String activityClassName, Object input, Class<V> returnType) {
    try {
      Class<?> activityClass = Class.forName(activityClassName);
      WorkflowActivity activity = (WorkflowActivity) activityClass.getDeclaredConstructor().newInstance();
      WorkflowActivityContext ctx = new WorkflowActivityContext(new TaskActivityContext() {

        @Override
        public java.lang.String getName() {
          return activityClassName;
        }

        @Override
        public <T> T getInput(Class<T> targetType) {
          return (T) input;
        }
      });

      randomFail();

      return (V) activity.run(ctx);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static int count = 0;
  private static Object countLock = new Object();

  public static class AddActivity implements WorkflowActivity, CompensatableWorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      Integer input = ctx.getInput(Integer.class);
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount + input;
        count = updatedCount;
      }
      String resultString = "current count is updated from " + originalCount + " to " + updatedCount
          + " after adding " + input;
      // System.out.println(resultString);
      return resultString;
    }

    @Override
    public void compensate(Object activityRequest) {
      int input = (Integer) activityRequest;
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount - input;
        count = updatedCount;
      }
      // System.out.println("current count is compensated from " + originalCount + "
      // to " + updatedCount + " after compensate adding " + input);
    }
  }

  public static class SubtractActivity implements WorkflowActivity, CompensatableWorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      Integer input = ctx.getInput(Integer.class);
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount - input;
        count = updatedCount;
      }
      String resultString = "current count is updated from " + originalCount + " to " + updatedCount
          + " after substracting " + input;
      // System.out.println(resultString);
      return resultString;
    }

    @Override
    public void compensate(Object activityRequest) {
      int input = (Integer) activityRequest;
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount + input;
        count = updatedCount;
      }
      // System.out.println("current count is compensated from " + originalCount + "
      // to " + updatedCount + " after compensate substracting " + input);
    }
  }

  public static class MultiplyActivity implements WorkflowActivity, CompensatableWorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      Integer input = ctx.getInput(Integer.class);
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount * input;
        count = updatedCount;
      }
      String resultString = "current count is updated from " + originalCount + " to " + updatedCount
          + " after multiplying " + input;
      // System.out.println(resultString);
      return resultString;
    }

    @Override
    public void compensate(Object activityRequest) {
      int input = (Integer) activityRequest;
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount / input;
        count = updatedCount;
      }
      // System.out.println("current count is compensated from " + originalCount + "
      // to " + updatedCount + " after compensate multiplying " + input);
    }
  }

  public static class DivideActivity implements WorkflowActivity, CompensatableWorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      Integer input = ctx.getInput(Integer.class);
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount / input;
        count = updatedCount;
      }
      String resultString = "current count is updated from " + originalCount + " to " + updatedCount
          + " after dividing " + input;
      // System.out.println(resultString);
      return resultString;
    }

    @Override
    public void compensate(Object activityInput) {
      int input = (Integer) activityInput;
      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount * input;
        count = updatedCount;
      }
      // System.out.println("current count is compensated from " + originalCount + "
      // to " + updatedCount + " after compensate dividing " + input);
    }
  }
}
