/*
 * Copyright 2023 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.examples.unittesting;

import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskCanceledException;
import io.dapr.workflows.runtime.Workflow;
import io.dapr.workflows.runtime.WorkflowContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run the test code:
 * java -jar target/dapr-java-sdk-examples-exec.jar \
 *     org.junit.platform.console.ConsoleLauncher --select-class=io.dapr.examples.unittesting.DaprWorkflowExampleTest
 */
public class DaprWorkflowExampleTest {

  private class DemoWorkflow extends Workflow {

    @Override
    public void run(WorkflowContext ctx) {
      String name = ctx.getName();
      String id = ctx.getInstanceId();
      try {
        ctx.waitForExternalEvent("myEvent", Duration.ofSeconds(10)).await();
      } catch (TaskCanceledException e) {
        ctx.getLogger().warn("Timed out");
      }
      String output = name + ":" + id;
      ctx.complete(output);
    }
  }

  @Test
  public void testWorkflow() {
    WorkflowContext mockContext = Mockito.mock(WorkflowContext.class);
    String name = "DemoWorkflow";
    String id = "my-workflow-123";

    Mockito.when(mockContext.getName()).thenReturn(name);
    Mockito.when(mockContext.getInstanceId()).thenReturn(id);
    Mockito.when(mockContext.waitForExternalEvent(anyString(),any(Duration.class)))
        .thenReturn(Mockito.mock(Task.class));

    new DemoWorkflow().run(mockContext);

    String expectedOutput = name + ":" + id;
    Mockito.verify(mockContext, Mockito.times(1)).complete(expectedOutput);
  }

  @Test
  public void testWorkflowWaitForEventTimeout() {
    WorkflowContext mockContext = Mockito.mock(WorkflowContext.class);
    Logger mockLogger = Mockito.mock(Logger.class);

    Mockito.when(mockContext.getLogger()).thenReturn(mockLogger);
    Mockito.when(mockContext.waitForExternalEvent(anyString(),any(Duration.class)))
        .thenThrow(TaskCanceledException.class);

    new DemoWorkflow().run(mockContext);

    Mockito.verify(mockLogger, Mockito.times(1)).warn("Timed out");
  }

  @Test
  public void testWorkflowWaitForEventNoTimeout() {
    WorkflowContext mockContext = Mockito.mock(WorkflowContext.class);
    Logger mockLogger = Mockito.mock(Logger.class);

    Mockito.when(mockContext.getLogger()).thenReturn(mockLogger);
    Mockito.when(mockContext.waitForExternalEvent(anyString(),any(Duration.class)))
        .thenReturn(Mockito.mock(Task.class));

    new DemoWorkflow().run(mockContext);

    Mockito.verify(mockLogger, Mockito.times(0)).warn(anyString());
  }
}
