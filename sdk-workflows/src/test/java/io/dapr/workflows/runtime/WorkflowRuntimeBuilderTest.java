package io.dapr.workflows.runtime;


import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class WorkflowRuntimeBuilderTest {
  public static class TestWorkflow extends Workflow {
    @Override
    public WorkflowStub create() {
      return ctx -> {
      };
    }
  }

  public static class TestActivity implements WorkflowActivity {
    @Override
    public Object run(WorkflowActivityContext ctx) {
      return null;
    }
  }

  @Test
  public void registerValidWorkflowClass() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerWorkflow(TestWorkflow.class));
  }

  @Test
  public void registerValidWorkflowActivityClass() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerActivity(TestActivity.class));
  }

  @Test
  public void buildTest() {
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().build());
  }

  @Test
  public void loggingOutputTest() {
    String DAPR_LOG_LEVEL = "INFO";
    // Set the output stream for log capturing
    ByteArrayOutputStream outStreamCapture = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStreamCapture));

    LogCaptureHandler testLoggerHandler = new LogCaptureHandler();
    Logger testLogger = Logger.getLogger(WorkflowRuntimeBuilder.class.getName());

    testLogger.addHandler(testLoggerHandler);

    // indexOf will return -1 if the string is not found.
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerWorkflow(TestWorkflow.class));
    assertNotEquals(-1, testLoggerHandler.capturedLog.indexOf("Registered Workflow: TestWorkflow"));
    assertDoesNotThrow(() -> new WorkflowRuntimeBuilder().registerActivity(TestActivity.class));
    assertNotEquals(-1, testLoggerHandler.capturedLog.indexOf("Registered Activity: TestActivity"));

    WorkflowRuntimeBuilder wfRuntime = new WorkflowRuntimeBuilder();

    wfRuntime.build();
  }

  private static class LogCaptureHandler extends Handler {
    private StringBuilder capturedLog = new StringBuilder();

    @Override
    public void publish(LogRecord record) {
      capturedLog.append(record.getMessage()).append(System.lineSeparator());
    }

    @Override
    public void flush(){
    }

    @Override
    public void close() throws SecurityException {
    }
  }
}
