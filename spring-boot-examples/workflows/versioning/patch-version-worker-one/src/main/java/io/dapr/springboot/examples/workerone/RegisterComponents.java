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

package io.dapr.springboot.examples.workerone;


import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.annotations.ActivityDefinition;
import io.dapr.workflows.annotations.WorkflowDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class RegisterComponents {

  @Component
  @WorkflowDefinition(name = "PatchVersionWorkflow")
  public static class PatchVersionWorkflowV1 implements Workflow {

    @Override
    public WorkflowStub create() {
      return ctx -> {
        String result = "";
        result += ctx.callActivity(Activity1.name, String.class).await() +", ";
        ctx.waitForExternalEvent("followup").await();
        result += ctx.callActivity(Activity2.name, String.class).await();

        ctx.complete(result);
      };
    }
  }

  @Component
  @ActivityDefinition(name = Activity1.name)
  public static class Activity1 implements WorkflowActivity {
    public static final String name = "Activity1";
    private final Logger logger = LoggerFactory.getLogger(Activity1.class);
    @Override
    public Object run(WorkflowActivityContext ctx) {
      logger.info(name + " started");
      return name;
    }
  }

  @Component
  @ActivityDefinition(name = Activity2.name)
  public static class Activity2 implements WorkflowActivity {
    public static final String name = "Activity2";
    private final Logger logger = LoggerFactory.getLogger(Activity2.class);
    @Override
    public Object run(WorkflowActivityContext ctx) {
      logger.info(name + " started");
      return name;
    }
  }

}
