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

package io.dapr.quarkus.langchain4j.scope;

import dev.langchain4j.agentic.scope.AgenticScopePersister;
import io.dapr.client.DaprClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Registers {@link DaprAgenticScopeStore} as LangChain4j's agentic scope persistence
 * provider at startup, so agentic workflow state (shared state, conversation context)
 * is checkpointed to a Dapr state store on every update.
 *
 * <p>Opt-in via configuration:
 * <pre>{@code
 * dapr.agentic.scope-store.enabled=true
 * dapr.agentic.scope-store.name=kvstore
 * }</pre>
 */
@ApplicationScoped
public class AgenticScopeStoreInitializer {

  private static final Logger LOG = Logger.getLogger(AgenticScopeStoreInitializer.class);

  @Inject
  DaprClient daprClient;

  @Inject
  @ConfigProperty(name = "dapr.agentic.scope-store.enabled", defaultValue = "false")
  boolean enabled;

  @Inject
  @ConfigProperty(name = "dapr.agentic.scope-store.name", defaultValue = "kvstore")
  String stateStoreName;

  void onStart(@Observes StartupEvent event) {
    if (!enabled) {
      return;
    }
    AgenticScopePersister.setStore(new DaprAgenticScopeStore(daprClient, stateStoreName));
    LOG.infof("Registered DaprAgenticScopeStore (state store: %s) — "
        + "agentic scopes are checkpointed to Dapr", stateStoreName);
  }
}
