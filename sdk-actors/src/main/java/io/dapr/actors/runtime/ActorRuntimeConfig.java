/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

class ActorRuntimeConfig {

  private Collection<String> registeredActorTypes = new ArrayList<>();

  ActorRuntimeConfig addRegisteredActorType(String actorTypeName) {
    if (actorTypeName == null) {
      throw new IllegalArgumentException("Registered actor must have a type name.");
    }

    this.registeredActorTypes.add(actorTypeName);
    return this;
  }

  Collection<String> getRegisteredActorTypes() {
    return Collections.unmodifiableCollection(registeredActorTypes);
  }

  ActorRuntimeConfig setRegisteredActorTypes(Collection<String> registeredActorTypes) {
    this.registeredActorTypes = registeredActorTypes;
    return this;
  }

}
