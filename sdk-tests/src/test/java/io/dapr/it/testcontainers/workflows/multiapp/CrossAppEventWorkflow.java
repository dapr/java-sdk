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

package io.dapr.it.testcontainers.workflows.multiapp;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.slf4j.Logger;

/**
 * Workflow that blocks on an external event so that a remote caller can observe it running,
 * suspend it, resume it, raise the event and terminate it.
 *
 * <p>The completion output carries the app ID of the app that actually ran the workflow, so a test
 * can assert which app hosted the instance.
 */
public class CrossAppEventWorkflow implements Workflow {

  public static final String CONTINUE_EVENT = "continue";

  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      String input = ctx.getInput(String.class);
      logger.info("CrossAppEventWorkflow {} started with input: {}", ctx.getInstanceId(), input);

      String eventPayload = ctx.waitForExternalEvent(CONTINUE_EVENT, String.class).await();
      logger.info("CrossAppEventWorkflow {} received event: {}", ctx.getInstanceId(), eventPayload);

      ctx.complete(input + " [" + eventPayload + "] [hosted by " + System.getProperty("dapr.app.id") + "]");
    };
  }
}
