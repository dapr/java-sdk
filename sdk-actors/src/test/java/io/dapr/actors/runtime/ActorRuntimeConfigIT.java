package io.dapr.actors.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.runtime.ActorRuntimeConfig;

import java.util.ArrayList;
import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActorRuntimeConfigIT extends ActorRuntimeConfig {

  private String actorIdleTimeout;

  private String actorScanInterval;

  private String drainOngoingCallTimeout;

  private Boolean drainBalancedActors;

  private Collection<String> entities = new ArrayList<>();

  public ActorRuntimeConfigIT(String actorIdleTimeout, String actorScanInterval, String drainOngoingCallTimeout, Boolean drainBalancedActors) {
    this.actorIdleTimeout = actorIdleTimeout;
    this.actorScanInterval = actorScanInterval;
    this.drainOngoingCallTimeout = drainOngoingCallTimeout;
    this.drainBalancedActors = drainBalancedActors;
  }

  public ActorRuntimeConfigIT addRegisteredActorType(String actorTypeName) {
    super.addRegisteredActorType(actorTypeName);
    this.entities.add(actorTypeName);
    return this;
  }

  public Collection<String> getRegisteredActorTypes() {
    return super.getRegisteredActorTypes();
  }

  public ActorRuntimeConfigIT setRegisteredActorTypes(Collection<String> registeredActorTypes) {
    super.setRegisteredActorTypes(registeredActorTypes);
    this.entities = registeredActorTypes;
    return this;
  }

  public String getActorIdleTimeout() {
    return actorIdleTimeout;
  }

  public void setActorIdleTimeout(String actorIdleTimeout) {
    this.actorIdleTimeout = actorIdleTimeout;
  }

  public String getActorScanInterval() {
    return actorScanInterval;
  }

  public void setActorScanInterval(String actorScanInterval) {
    this.actorScanInterval = actorScanInterval;
  }

  public String getDrainOngoingCallTimeout() {
    return drainOngoingCallTimeout;
  }

  public void setDrainOngoingCallTimeout(String drainOngoingCallTimeout) {
    this.drainOngoingCallTimeout = drainOngoingCallTimeout;
  }

  public Boolean getDrainBalancedActors() {
    return drainBalancedActors;
  }

  public void setDrainBalancedActors(Boolean drainBalancedActors) {
    this.drainBalancedActors = drainBalancedActors;
  }

  public Collection<String> getEntities() {
    return entities;
  }

  public void setEntities(Collection<String> entities) {
    this.entities = entities;
  }

  public String serializedConfig() {
    ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);;
    io.dapr.client.ObjectSerializer serializer = new ObjectSerializer();
    try {
      return mapper.writeValueAsString(this);
    } catch (Exception ex) {
      return "";
    }
  }
}
