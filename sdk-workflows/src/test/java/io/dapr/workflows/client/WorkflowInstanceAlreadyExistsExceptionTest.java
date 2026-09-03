/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.workflows.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class WorkflowInstanceAlreadyExistsExceptionTest {

  @Test
  public void messageIncludesInstanceIdWhenKnown() {
    StatusRuntimeException cause = new StatusRuntimeException(Status.ALREADY_EXISTS);

    WorkflowInstanceAlreadyExistsException exception =
        new WorkflowInstanceAlreadyExistsException("myInstance", cause);

    assertEquals("a workflow with ID 'myInstance' already exists", exception.getMessage());
    assertEquals("myInstance", exception.getInstanceId());
    assertSame(cause, exception.getCause());
  }

  @Test
  public void messageIsGenericWhenInstanceIdIsUnknown() {
    StatusRuntimeException cause = new StatusRuntimeException(Status.ALREADY_EXISTS);

    WorkflowInstanceAlreadyExistsException exception =
        new WorkflowInstanceAlreadyExistsException(null, cause);

    assertEquals("a workflow with the requested instance ID already exists", exception.getMessage());
    assertNull(exception.getInstanceId());
    assertSame(cause, exception.getCause());
  }
}
