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

package io.dapr.springboot.examples.wfp.remoteendpoint;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RemoteEndpointWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      Payload payload = ctx.getInput(Payload.class);
      payload = ctx.callActivity(CallRemoteEndpointActivity.class.getName(), payload ,
              new WorkflowTaskOptions(new WorkflowTaskRetryPolicy(5,
                      Duration.ofSeconds(2), 1.0, Duration.ofSeconds(10), Duration.ofSeconds(20))),
              Payload.class).await();

      ctx.getLogger().info("Workflow finished with result: " + payload);
      ctx.complete(payload);
    };
  }
}
