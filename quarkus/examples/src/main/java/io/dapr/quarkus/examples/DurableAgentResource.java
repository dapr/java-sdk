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

package io.dapr.quarkus.examples;

import io.dapr.quarkus.langchain4j.durable.ReActInput;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates starting the durable {@link io.dapr.quarkus.langchain4j.durable.ReActAgentWorkflow}
 * directly via the Dapr workflow client (the agent's ReAct loop runs <em>as</em> a workflow).
 *
 * <p>Composites are exercised through the drop-in entry point (just inject {@code @SequenceAgent}
 * etc.); these endpoints only show the leaf react-agent path.
 *
 * <pre>
 * curl "http://localhost:8080/durable?topic=dragons"
 * curl "http://localhost:8080/durable/research?country=France"
 * </pre>
 */
@Path("/durable")
public class DurableAgentResource {

  @Inject
  DaprWorkflowClient workflowClient;

  /**
   * Starts the durable ReAct workflow for a single creative-writer agent and returns its result.
   *
   * @param topic the story topic
   * @return the generated story text
   * @throws TimeoutException if the workflow does not complete within the wait window
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String run(@QueryParam("topic") @DefaultValue("dragons and wizards") String topic)
      throws TimeoutException {
    String userMessage = "You are a creative writer. Generate a draft of a story no more than "
        + "3 sentences around the topic '" + topic + "'. Return only the story and nothing else.";
    return runReactAgent("creative-writer-agent", userMessage, "durable-");
  }

  /**
   * Starts the durable ReAct workflow for a tool-using research agent and returns its result.
   *
   * <p>Exercises the {@code agent-tool} activity: the model requests a tool call, the tool runs
   * as a replica-agnostic activity, and its result is fed back into the loop.
   *
   * @param country the country to research
   * @return the research summary
   * @throws TimeoutException if the workflow does not complete within the wait window
   */
  @GET
  @Path("/research")
  @Produces(MediaType.TEXT_PLAIN)
  public String research(@QueryParam("country") @DefaultValue("France") String country)
      throws TimeoutException {
    String userMessage = "You are a research assistant. Write a concise summary about the country "
        + country + " using the available tools. Return only the summary.";
    return runReactAgent("research-location-agent", userMessage, "durable-research-");
  }

  private String runReactAgent(String agentName, String userMessage, String idPrefix)
      throws TimeoutException {
    ReActInput input = new ReActInput(agentName, null, userMessage, null, null, 8);
    String instanceId = idPrefix + UUID.randomUUID();
    workflowClient.scheduleNewWorkflow("react-agent", input, instanceId);
    WorkflowInstanceStatus status =
        workflowClient.waitForInstanceCompletion(instanceId, Duration.ofSeconds(60), true);
    return status.readOutputAs(String.class);
  }
}
