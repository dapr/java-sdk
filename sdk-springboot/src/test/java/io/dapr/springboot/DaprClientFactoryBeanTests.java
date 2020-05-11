/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientHttp;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DaprClientFactoryBeanTests {

  @Configuration
  static class MyTestConfiguration {

    // tests specific beans
    @Bean(name = "client")
    public DaprClientFactoryBean daprClientFactoryBean() {
      DaprClientFactoryBean factory = new DaprClientFactoryBean();
      factory.setUseGrpc(false);
      return factory;
    }

    @Bean(name = "objectSerializer")
    public DaprObjectSerializer createDaprObjectSerializer() {
      return new DefaultObjectSerializer();
    }
  }

  @Resource(name = "&client")
  private DaprClientFactoryBean daprClientFactory;

  @Autowired
  private DaprClient daprClient;

  @Test
  public void testBasicCration() {
    assertNotNull(this.daprClient);
    assertNotNull(this.daprClientFactory);
    assertTrue(this.daprClient instanceof DaprClientHttp);
    assertNotNull(this.daprClientFactory.getObjectSerializer());
    assertEquals(null, this.daprClientFactory.getStateSerializer());
  }
}