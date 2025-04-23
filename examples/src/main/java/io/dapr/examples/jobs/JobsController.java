/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.examples.jobs;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * SpringBoot Controller to handle jobs callback.
 */
@RestController
public class  JobsController {

  /**
   * Handles jobs callback from Dapr.
   *
   * @param jobName name of the job.
   * @param payload data from the job if payload exists.
   * @return Empty Mono.
   */
  @PostMapping("/job/{jobName}")
  public Mono<Void> handleJob(@PathVariable("jobName") String jobName,
                              @RequestBody(required = false) byte[] payload) {
    System.out.println("Job Name: " + jobName);
    System.out.println("Job Payload: " + new String(payload));

    return Mono.empty();
  }
}
