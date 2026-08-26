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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkflowInstancePageTest {

  @Test
  public void exposesInstanceIdsAndToken() {
    WorkflowInstancePage page = new WorkflowInstancePage(Arrays.asList("a", "b"), "next");
    assertEquals(Arrays.asList("a", "b"), page.getInstanceIds());
    assertEquals("next", page.getContinuationToken());
  }

  @Test
  public void allowsNullContinuationToken() {
    WorkflowInstancePage page = new WorkflowInstancePage(Arrays.asList("a"), null);
    assertNull(page.getContinuationToken());
  }

  @Test
  public void instanceIdsListIsUnmodifiable() {
    WorkflowInstancePage page = new WorkflowInstancePage(Arrays.asList("a"), null);
    assertThrows(UnsupportedOperationException.class, () -> page.getInstanceIds().add("b"));
  }
}
