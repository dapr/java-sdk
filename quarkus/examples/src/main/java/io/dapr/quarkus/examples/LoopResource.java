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

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint that triggers the loop creation workflow.
 *
 * <p>Runs {@link CreativeWriter} and {@link StyleEditor} twice in a loop via a
 * {@code LoopOrchestrationWorkflow} Dapr Workflow.
 *
 * <p>Example usage:
 * <pre>
 * curl "http://localhost:8080/loop?topic=dragons&amp;style=comedy"
 * </pre>
 */
@Path("/loop")
public class LoopResource {

  @Inject
  LoopWriter loopWriter;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String create(
      @QueryParam("topic") @DefaultValue("dragons and wizards") String topic,
      @QueryParam("style") @DefaultValue("comedy") String style) {
    return loopWriter.write(topic, style);
  }
}
