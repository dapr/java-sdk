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
 * REST endpoint that triggers a research workflow with tool calls routed through
 * Dapr Workflow Activities.
 *
 * <p>Each request:
 * <ol>
 *   <li>Starts a {@code SequentialOrchestrationWorkflow} (orchestration level).</li>
 *   <li>For the {@link ResearchWriter} sub-agent, starts an {@code AgentRunWorkflow}
 *       (per-agent level).</li>
 *   <li>Each LLM tool call ({@code getPopulation} / {@code getCapital}) is executed
 *       inside a {@code ToolCallActivity} (tool-call level).</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>
 * curl "http://localhost:8080/research?country=France"
 * curl "http://localhost:8080/research?country=Germany"
 * </pre>
 */
@Path("/research")
public class ResearchResource {

  @Inject
  ResearchWriter researchWriter;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String research(
      @QueryParam("country") @DefaultValue("France") String country) {
    return researchWriter.research(country);
  }
}
