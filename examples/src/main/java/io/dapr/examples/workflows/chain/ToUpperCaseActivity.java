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

package io.dapr.examples.workflows.chain;

import io.dapr.workflows.runtime.WorkflowActivity;
import io.dapr.workflows.runtime.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ToUpperCaseActivity implements WorkflowActivity {

  @Override
  public Object run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(ToUpperCaseActivity.class);
    logger.info("Starting Activity: " + ctx.getName());

    var message = ctx.getInput(String.class);
    var newMessage = message.toUpperCase();

    logger.info("Message Received from input: " + message);
    logger.info("Sending message to output: " + newMessage);

    return newMessage;
  }
}
