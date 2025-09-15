/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.springboot.examples.wfp.timer;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class ZonedDateTimeTimerWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: {}, instanceId: {}", ctx.getName(), ctx.getInstanceId());

      ctx.getLogger().info("Let's call the first LogActivity at {}", new Date());
      ctx.callActivity(LogActivity.class.getName()).await();

      ZonedDateTime now = ZonedDateTime.now();
      //Let's create a ZonedDateTime 10 seconds in the future
      ZonedDateTime inTheFuture = now.plusSeconds(10);
      ctx.getLogger().info("Creating a timer that due {} at: {}", inTheFuture, new Date());
      ctx.createTimer(inTheFuture).await();
      ctx.getLogger().info("The timer fired at: {}", new Date());

      ctx.getLogger().info("Let's call the second LogActivity at {}", new Date());
      ctx.callActivity(LogActivity.class.getName()).await();

      ctx.complete(true);
      ctx.getLogger().info("Workflow completed at {}", new Date());
    };
  }
}
