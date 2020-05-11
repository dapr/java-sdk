/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.serializer.DaprObjectSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class DaprClientFactoryBean extends AbstractFactoryBean<DaprClient> {

  private boolean useGrpc = true;

  @Autowired(required = false)
  @Qualifier("objectSerializer")
  private DaprObjectSerializer objectSerializer;

  @Autowired(required = false)
  @Qualifier("stateSerializer")
  private DaprObjectSerializer stateSerializer;

  @Override
  protected DaprClient createInstance() throws Exception {

    DaprClientBuilder builder = new DaprClientBuilder();
    builder.setUseGrpc(this.useGrpc);

    if (this.objectSerializer != null) {
      builder.withObjectSerializer(this.objectSerializer);
    }
    if (this.stateSerializer != null) {
      builder.withStateSerializer(this.stateSerializer);
    }

    return builder.build();
  }

  @Override
  public Class<?> getObjectType() {
    return DaprClient.class;
  }

  public DaprObjectSerializer getObjectSerializer() {
    return objectSerializer;
  }

  public void setObjectSerializer(DaprObjectSerializer objectSerializer) {
    this.objectSerializer = objectSerializer;
  }

  public DaprObjectSerializer getStateSerializer() {
    return stateSerializer;
  }

  public void setStateSerializer(DaprObjectSerializer stateSerializer) {
    this.stateSerializer = stateSerializer;
  }

  public boolean isUseGrpc() {
    return useGrpc;
  }

  public void setUseGrpc(boolean useGrpc) {
    this.useGrpc = useGrpc;
  }
}