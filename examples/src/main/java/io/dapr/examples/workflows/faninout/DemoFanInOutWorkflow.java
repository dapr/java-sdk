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

package io.dapr.examples.workflows.faninout;

import com.microsoft.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

import java.util.List;
import java.util.stream.Collectors;

public class DemoFanInOutWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {

      ctx.getLogger().info("Starting Workflow: " + ctx.getName());


      // The input is a list of objects that need to be operated on.
      // In this example, inputs are expected to be strings.
      List<?> inputs = ctx.getInput(List.class);

      // Fan-out to multiple concurrent activity invocations, each of which does a word count.
      List<Task<Integer>> tasks = inputs.stream()
          .map(input -> ctx.callActivity(CountWordsActivity.class.getName(), input.toString(), Integer.class))
          .collect(Collectors.toList());

      // Fan-in to get the total word count from all the individual activity results.
      List<Integer> allWordCountResults = ctx.allOf(tasks).await();
      int totalWordCount = allWordCountResults.stream().mapToInt(Integer::intValue).sum();

      ctx.getLogger().info("Workflow finished with result: " + totalWordCount);
      // Save the final result as the orchestration output.
      ctx.complete(totalWordCount);
    };
  }
}
