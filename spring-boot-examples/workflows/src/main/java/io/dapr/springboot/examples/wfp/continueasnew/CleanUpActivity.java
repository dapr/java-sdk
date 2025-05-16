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

package io.dapr.springboot.examples.wfp.continueasnew;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class CleanUpActivity implements WorkflowActivity {

  @Autowired
  private CleanUpLog cleanUpLog;

  @Override
  public Object run(WorkflowActivityContext ctx) {
    Logger logger = LoggerFactory.getLogger(CleanUpActivity.class);
    logger.info("Starting Activity: " + ctx.getName());

    LocalDateTime now = LocalDateTime.now();
    String cleanUpTimeString = now.getHour() + ":" + now.getMinute() + ":" + now.getSecond();
    logger.info("start clean up work, it may take few seconds to finish... Time:" + cleanUpTimeString);

    //Sleeping for 2 seconds to simulate long running operation
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    cleanUpLog.increment();

    return "clean up finish.";
  }
}
