/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.client;

import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.grpc.Grpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DaprClientBuilderTest {

  @Test
  public void build() {
    DaprObjectSerializer objectSerializer = mock(DaprObjectSerializer.class);
    when(objectSerializer.getContentType()).thenReturn("application/json");
    DaprObjectSerializer stateSerializer = mock(DaprObjectSerializer.class);
    DaprClientBuilder daprClientBuilder = new DaprClientBuilder();
    daprClientBuilder.withObjectSerializer(objectSerializer);
    daprClientBuilder.withStateSerializer(stateSerializer);
    DaprClient daprClient = daprClientBuilder.build();
    assertNotNull(daprClient);
  }

  @Test
  public void testBuilderWithManagedChannel(){

    System.setProperty(Properties.API_METHOD_INVOCATION_PROTOCOL.getName(), DaprApiProtocol.GRPC.name());
    System.setProperty(Properties.API_PROTOCOL.getName(),DaprApiProtocol.GRPC.name());
    DaprExtensibleClientBuilder daprClientBuilder = new DaprExtensibleClientBuilder();
    ManagedChannelBuilder managedChannelBuilder = Grpc.newChannelBuilder(Properties.SIDECAR_IP.get(), TlsChannelCredentials.create())
            .executor(Executors.newFixedThreadPool(20));
    DaprClient client = daprClientBuilder.withManagedGrpcChannel(managedChannelBuilder.build()).build();
    assertNotNull(client);
    System.setProperty(Properties.API_PROTOCOL.getName(),"");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testBuilderWithManagedChannelErrorHandling(){
    System.setProperty(Properties.API_METHOD_INVOCATION_PROTOCOL.getName(), DaprApiProtocol.HTTP.name());
    System.setProperty(Properties.API_PROTOCOL.getName(),DaprApiProtocol.HTTP.name());
    DaprExtensibleClientBuilder daprClientBuilder = new DaprExtensibleClientBuilder();
    ManagedChannelBuilder managedChannelBuilder = Grpc.newChannelBuilder(Properties.SIDECAR_IP.get(), TlsChannelCredentials.create())
            .executor(Executors.newFixedThreadPool(20));
    DaprClient client = daprClientBuilder.withManagedGrpcChannel(managedChannelBuilder.build()).build();
    assertNotNull(client);
    System.setProperty(Properties.API_PROTOCOL.getName(),"");
  }

  @Test(expected = IllegalArgumentException.class)
  public void noObjectSerializer() {
    new DaprClientBuilder().withObjectSerializer(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void blankContentTypeInObjectSerializer() {
    new DaprClientBuilder().withObjectSerializer(mock(DaprObjectSerializer.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void noStateSerializer() {
    new DaprClientBuilder().withStateSerializer(null);
  }

}
