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

package io.quarkiverse.dapr.examples;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that triggers the parallel creation workflow.
 *
 * <p>Runs {@link StoryCreator} (a nested {@code @SequenceAgent}) and {@link ResearchWriter}
 * in parallel via a {@code ParallelOrchestrationWorkflow} Dapr Workflow.
 *
 * <p>Example usage:
 * <pre>
 * curl "http://localhost:8080/parallel?topic=dragons&amp;country=France&amp;style=comedy"
 * </pre>
 */
@Path("/parallel")
public class ParallelResource {

  @Inject
  ParallelCreator parallelCreator;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ParallelStatus create(
      @QueryParam("topic") @DefaultValue("dragons and wizards") String topic,
      @QueryParam("country") @DefaultValue("France") String country,
      @QueryParam("style") @DefaultValue("fantasy") String style) {
    return parallelCreator.create(topic, country, style);
  }
}
