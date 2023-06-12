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

package io.dapr.workflows.client;


import com.microsoft.durabletask.OrchestrationRuntimeStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class WorkflowRuntimeStatusTest {

  @Before
  public void setUp() throws Exception {
   
  }

  @Test
  public void fromOrchestrationRuntimeStatus() {

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.RUNNING), 
        WorkflowRuntimeStatus.RUNNING);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.COMPLETED), 
        WorkflowRuntimeStatus.COMPLETED);

    Assert.assertEquals(
        WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.CONTINUED_AS_NEW), 
        WorkflowRuntimeStatus.CONTINUED_AS_NEW);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.FAILED), 
        WorkflowRuntimeStatus.FAILED);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.CANCELED), 
        WorkflowRuntimeStatus.CANCELED);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.TERMINATED), 
        WorkflowRuntimeStatus.TERMINATED);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.PENDING), 
        WorkflowRuntimeStatus.PENDING);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.SUSPENDED), 
        WorkflowRuntimeStatus.SUSPENDED);

    Assert.assertEquals(WorkflowRuntimeStatus.fromOrchestrationRuntimeStatus(null), 
        WorkflowRuntimeStatus.UNKNOWN);

  }

  @Test
  public void toOrchestrationRuntimeStatus() {
    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.RUNNING), 
        OrchestrationRuntimeStatus.RUNNING);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.COMPLETED), 
        OrchestrationRuntimeStatus.COMPLETED);

    Assert.assertEquals(
        WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.CONTINUED_AS_NEW), 
        OrchestrationRuntimeStatus.CONTINUED_AS_NEW);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.FAILED), 
        OrchestrationRuntimeStatus.FAILED);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.CANCELED), 
        OrchestrationRuntimeStatus.CANCELED);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.TERMINATED), 
        OrchestrationRuntimeStatus.TERMINATED);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.PENDING), 
        OrchestrationRuntimeStatus.PENDING);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.SUSPENDED), 
        OrchestrationRuntimeStatus.SUSPENDED);

    Assert.assertEquals(WorkflowRuntimeStatus.toOrchestrationRuntimeStatus(WorkflowRuntimeStatus.UNKNOWN), 
          null);

  }
}
