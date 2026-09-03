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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

public class RerunWorkflowFromEventOptionsTest {

  @Test
  public void fluentSettersReturnSameInstanceAndStoreValues() {
    RerunWorkflowFromEventOptions options = new RerunWorkflowFromEventOptions();
    assertSame(options, options.setNewInstanceId("target"));
    assertSame(options, options.setInput("payload"));
    assertSame(options, options.setOverwriteInput(true));

    assertEquals("target", options.getNewInstanceId());
    assertEquals("payload", options.getInput());
    assertEquals(true, options.isOverwriteInput());
  }

  @Test
  public void overwriteInputDefaultsToFalse() {
    assertFalse(new RerunWorkflowFromEventOptions().isOverwriteInput());
  }
}
