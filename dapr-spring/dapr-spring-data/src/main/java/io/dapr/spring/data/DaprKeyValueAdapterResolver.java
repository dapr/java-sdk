/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.spring.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.ComponentMetadata;
import io.dapr.client.domain.DaprMetadata;
import org.springframework.data.keyvalue.core.KeyValueAdapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DaprKeyValueAdapterResolver implements KeyValueAdapterResolver {
  private static final List<String> MYSQL_VALUES = Arrays.asList("state.mysql-v1", "bindings.mysql-v1");
  private static final Set<String> MYSQL_MARKERS = new HashSet<>(MYSQL_VALUES);
  private static final List<String> POSTGRESQL_VALUES = Arrays.asList("state.postgresql-v1", "bindings.postgresql-v1");
  private static final Set<String> POSTGRESQL_MARKERS = new HashSet<>(POSTGRESQL_VALUES);
  private final DaprClient daprClient;
  private final ObjectMapper mapper;
  private final String stateStoreName;
  private final String bindingName;

  /**
   * Constructs a {@link DaprKeyValueAdapterResolver}.
   *
   * @param daprClient     The Dapr client.
   * @param mapper         The object mapper.
   * @param stateStoreName The state store name.
   * @param bindingName    The binding name.
   */
  public DaprKeyValueAdapterResolver(DaprClient daprClient, ObjectMapper mapper, String stateStoreName,
                                     String bindingName) {
    this.daprClient = daprClient;
    this.mapper = mapper;
    this.stateStoreName = stateStoreName;
    this.bindingName = bindingName;
  }

  @Override
  public KeyValueAdapter resolve() {
    DaprMetadata metadata = daprClient.getMetadata().block();

    if (metadata == null) {
      throw new IllegalStateException("No Dapr metadata found");
    }

    List<ComponentMetadata> components = metadata.getComponents();

    if (components == null || components.isEmpty()) {
      throw new IllegalStateException("No components found in Dapr metadata");
    }

    if (shouldUseMySQL(components, stateStoreName, bindingName)) {
      return new MySQLDaprKeyValueAdapter(daprClient, mapper, stateStoreName, bindingName);
    }

    if (shouldUsePostgreSQL(components, stateStoreName, bindingName)) {
      return new PostgreSQLDaprKeyValueAdapter(daprClient, mapper, stateStoreName, bindingName);
    }

    throw new IllegalStateException("Could find any adapter matching the given state store and binding");
  }

  @SuppressWarnings("AbbreviationAsWordInName")
  private boolean shouldUseMySQL(List<ComponentMetadata> components, String stateStoreName, String bindingName) {
    boolean stateStoreMatched = components.stream().anyMatch(x -> matchBy(stateStoreName, MYSQL_MARKERS, x));
    boolean bindingMatched = components.stream().anyMatch(x -> matchBy(bindingName, MYSQL_MARKERS, x));

    return stateStoreMatched && bindingMatched;
  }

  @SuppressWarnings("AbbreviationAsWordInName")
  private boolean shouldUsePostgreSQL(List<ComponentMetadata> components, String stateStoreName, String bindingName) {
    boolean stateStoreMatched = components.stream().anyMatch(x -> matchBy(stateStoreName, POSTGRESQL_MARKERS, x));
    boolean bindingMatched = components.stream().anyMatch(x -> matchBy(bindingName, POSTGRESQL_MARKERS, x));

    return stateStoreMatched && bindingMatched;
  }

  private boolean matchBy(String name, Set<String> markers, ComponentMetadata componentMetadata) {
    return componentMetadata.getName().equals(name) && markers.contains(getTypeAndVersion(componentMetadata));
  }

  private String getTypeAndVersion(ComponentMetadata component) {
    return component.getType() + "-" + component.getVersion();
  }
}
