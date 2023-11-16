package io.dapr.workflows.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.microsoft.durabletask.TaskActivityContext;

import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;

public class SagaIntegrationTest {

  private static int count = 0;
  private static Object countLock = new Object();

  @Test
  public void testSaga_CompensateSequentially() {
    int runCount = 10;
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
    SagaConfiguration config = SagaConfiguration.newBuilder()
        .setParallelCompensation(parallelCompensation)
        .setContinueWithError(true).build();
    Saga saga = new Saga(config);
    boolean workflowSuccess = false;

    // reset count to zero
    synchronized(countLock) {
      count = 0;
    }
    
    Integer addInput = 100;
    Integer subtractInput = 20;
    Integer multiplyInput = 10;
    Integer divideInput = 5;

    try {
      // step1: add activity
      String result = callActivity(AddActivity.class.getName(), addInput, String.class);
      saga.registerCompensation(AddCompentationActivity.class.getName(), addInput, result);
      // step2: subtract activity
      result = callActivity(SubtractActivity.class.getName(), subtractInput, String.class);
      saga.registerCompensation(SubtractCompentationActivity.class.getName(), subtractInput, result);

      if (parallelCompensation) {
        // only add/subtract activities support parallel compensation
        // so in step3 and step4 we repeat add/subtract activities

        // step3: add activity again
        result = callActivity(AddActivity.class.getName(), addInput, String.class);
        saga.registerCompensation(AddCompentationActivity.class.getName(), addInput, result);

        // step4: substract activity again
        result = callActivity(SubtractActivity.class.getName(), subtractInput, String.class);
        saga.registerCompensation(SubtractCompentationActivity.class.getName(), subtractInput, result);
      } else {
        // step3: multiply activity
        result = callActivity(MultiplyActivity.class.getName(), multiplyInput, String.class);
        saga.registerCompensation(MultiplyCompentationActivity.class.getName(), multiplyInput, result);

        // step4: divide activity
        result = callActivity(DivideActivity.class.getName(), divideInput, String.class);
        saga.registerCompensation(DivideCompentationActivity.class.getName(), divideInput, result);
      }

      randomFail();

      workflowSuccess = true;
    } catch (Exception e) {
      saga.compensate(new SagaTest.MockWorkflowContext());
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

  // mock to call activity in dapr workflow
  private <V> V callActivity(String activityClassName, Object input, Class<V> returnType) {
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

  private static void randomFail() {
    int randomInt = (int) (Math.random() * 100);
    // if randomInt mod 10 is 0, then throw exception
    if (randomInt % 10 == 0) {
      throw new RuntimeException("random fail");
    }
  }

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
    public Class<? extends WorkflowActivity> getCompensationActivity() {
      return AddCompentationActivity.class;
    }
  }

  public static class AddCompentationActivity implements WorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      CompensatationContext compensatationContext = ctx.getInput(CompensatationContext.class);
      Integer input = (Integer) compensatationContext.getActivityInput();
      String output = (String)compensatationContext.getActivityOutput();

      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount - input;
        count = updatedCount;
      }

      String resultString = "current count is compensated from " + originalCount + " to "
          + updatedCount + " after compensate adding " + input;
      // System.out.println(resultString);
      return resultString;
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
    public Class<? extends WorkflowActivity> getCompensationActivity() {
      return SubtractCompentationActivity.class;
    }
  }

  public static class SubtractCompentationActivity implements WorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      CompensatationContext compensatationContext = ctx.getInput(CompensatationContext.class);
      Integer input = (Integer) compensatationContext.getActivityInput();
      String output = (String)compensatationContext.getActivityOutput();

      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount + input;
        count = updatedCount;
      }

      String resultString = "current count is compensated from " + originalCount + " to " + updatedCount
          + " after compensate substracting " + input;
      // System.out.println(resultString);
      return resultString;
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
    public Class<? extends WorkflowActivity> getCompensationActivity() {
      return MultiplyCompentationActivity.class;
    }
  }

  public static class MultiplyCompentationActivity implements WorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      CompensatationContext compensatationContext = ctx.getInput(CompensatationContext.class);
      Integer input = (Integer) compensatationContext.getActivityInput();
            String output = (String)compensatationContext.getActivityOutput();


      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount / input;
        count = updatedCount;
      }

      String resultString = "current count is compensated from " + originalCount + " to " + updatedCount
          + " after compensate multiplying " + input;
      // System.out.println(resultString);
      return resultString;
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
    public Class<? extends WorkflowActivity> getCompensationActivity() {
      return DivideCompentationActivity.class;
    }
  }

  public static class DivideCompentationActivity implements WorkflowActivity {

    @Override
    public String run(WorkflowActivityContext ctx) {
      CompensatationContext compensatationContext = ctx.getInput(CompensatationContext.class);
      Integer input = (Integer) compensatationContext.getActivityInput();
            String output = (String)compensatationContext.getActivityOutput();


      int originalCount = 0;
      int updatedCount = 0;
      synchronized (countLock) {
        originalCount = count;
        updatedCount = originalCount * input;
        count = updatedCount;
      }

      String resultString = "current count is compensated from " + originalCount + " to " + updatedCount
          + " after compensate dividing " + input;
      // System.out.println(resultString);
      return resultString;
    }
  }
}
