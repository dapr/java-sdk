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
limitations under the License.
*/

package io.dapr.workflows.task.exception;

import io.dapr.workflows.task.history.PropagatedHistoryException;
import io.dapr.workflows.task.interruption.ContinueAsNewInterruption;
import io.dapr.workflows.task.interruption.OrchestratorBlockedException;
import io.dapr.workflows.task.serialization.DataConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkflowFailureDetailsTest {

  private static WorkflowFailureDetails failureOfType(String errorType) {
    return new WorkflowFailureDetails(errorType, "boom", "", false);
  }

  @Test
  public void isCausedByResolvesACurrentErrorType() {
    assertTrue(failureOfType(TaskFailedException.class.getName()).isCausedBy(TaskFailedException.class));
  }

  @Test
  public void isCausedByHonoursTheExceptionHierarchy() {
    // TaskCanceledException extends TaskFailedException
    assertTrue(failureOfType(TaskCanceledException.class.getName()).isCausedBy(TaskFailedException.class));
    assertTrue(failureOfType(TaskFailedException.class.getName()).isCausedBy(RuntimeException.class));
  }

  /**
   * The error type is the exception's fully qualified name and it is persisted into workflow
   * history. These names shipped under io.dapr.durabletask before the durable task client was
   * folded into this module, so a workflow started on an older SDK carries them across an upgrade.
   * Without the legacy mapping isCausedBy answered false and compensation logic took the wrong
   * branch, with nothing logged and nothing thrown.
   */
  @Test
  public void legacyNamesMapOntoTheirCurrentTypes() {
    assertTrue(failureOfType("io.dapr.durabletask.TaskFailedException")
        .isCausedBy(TaskFailedException.class));
    assertTrue(failureOfType("io.dapr.durabletask.TaskCanceledException")
        .isCausedBy(TaskCanceledException.class));
    assertTrue(failureOfType("io.dapr.durabletask.CompositeTaskFailedException")
        .isCausedBy(CompositeTaskFailedException.class));
    assertTrue(failureOfType("io.dapr.durabletask.NonDeterministicOrchestratorException")
        .isCausedBy(NonDeterministicOrchestratorException.class));
    assertTrue(failureOfType("io.dapr.durabletask.PropagatedHistoryException")
        .isCausedBy(PropagatedHistoryException.class));
    assertTrue(failureOfType("io.dapr.durabletask.orchestration.exception.VersionNotRegisteredException")
        .isCausedBy(VersionNotRegisteredException.class));
    assertTrue(failureOfType("io.dapr.durabletask.interruption.OrchestratorBlockedException")
        .isCausedBy(OrchestratorBlockedException.class));
    assertTrue(failureOfType("io.dapr.durabletask.interruption.ContinueAsNewInterruption")
        .isCausedBy(ContinueAsNewInterruption.class));
  }

  /**
   * DataConverterException is nested inside the DataConverter interface, so its binary name uses
   * '$' and it has no source file of its own. Enumerating the legacy exception types by file name
   * missed it, leaving serialization failures persisted by the old SDK unresolvable.
   */
  @Test
  public void theNestedConverterExceptionIsMappedToo() {
    assertTrue(failureOfType("io.dapr.durabletask.DataConverter$DataConverterException")
        .isCausedBy(DataConverter.DataConverterException.class));
  }

  /**
   * An unresolvable error type answers false to EVERY query, not just the exact-type check, so a
   * missing legacy entry also breaks the broad catch-all questions callers most often ask.
   */
  @Test
  public void theNestedConverterExceptionAlsoAnswersBroaderQueries() {
    WorkflowFailureDetails details =
        failureOfType("io.dapr.durabletask.DataConverter$DataConverterException");

    assertTrue(details.isCausedBy(RuntimeException.class));
    assertTrue(details.isCausedBy(Exception.class));
  }

  @Test
  public void aLegacyNameDoesNotMatchAnUnrelatedType() {
    assertFalse(failureOfType("io.dapr.durabletask.TaskFailedException")
        .isCausedBy(IllegalStateException.class));
  }

  /**
   * A user exception that happens to share a simple name with an SDK one must not match. This is
   * why the fix is an explicit alias table rather than a simple-name comparison.
   */
  @Test
  public void anUnrelatedTypeWithTheSameSimpleNameDoesNotMatch() {
    assertFalse(failureOfType("com.example.TaskFailedException").isCausedBy(TaskFailedException.class));
  }

  @Test
  public void anUnloadableErrorTypeAnswersFalse() {
    assertFalse(failureOfType("com.example.NotOnTheClasspath").isCausedBy(RuntimeException.class));
  }
}
