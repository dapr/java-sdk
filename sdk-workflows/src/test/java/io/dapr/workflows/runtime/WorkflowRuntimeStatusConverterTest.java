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

package io.dapr.workflows.runtime;

import io.dapr.durabletask.OrchestrationRuntimeStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class WorkflowRuntimeStatusConverterTest {

  @Test
  public void fromOrchestrationRuntimeStatus() {

    assertEquals(WorkflowRuntimeStatus.RUNNING,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.RUNNING)
    );

    assertEquals(WorkflowRuntimeStatus.COMPLETED,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.COMPLETED)
    );

    assertEquals(WorkflowRuntimeStatus.CONTINUED_AS_NEW,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.CONTINUED_AS_NEW)
    );

    assertEquals(WorkflowRuntimeStatus.FAILED,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.FAILED)
    );

    assertEquals(WorkflowRuntimeStatus.CANCELED,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.CANCELED)
    );

    assertEquals(WorkflowRuntimeStatus.TERMINATED,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.TERMINATED)
    );

    assertEquals(WorkflowRuntimeStatus.PENDING,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.PENDING)
    );

    assertEquals(WorkflowRuntimeStatus.SUSPENDED,
        WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(OrchestrationRuntimeStatus.SUSPENDED)
    );
  }

  @Test
  public void fromOrchestrationRuntimeStatusThrowsIllegalArgumentException() {
    try {
      WorkflowRuntimeStatusConverter.fromOrchestrationRuntimeStatus(null);

      fail("Expected exception not thrown");
    } catch (IllegalArgumentException e) {
      assertEquals("status cannot be null", e.getMessage());
    }
  }
}
