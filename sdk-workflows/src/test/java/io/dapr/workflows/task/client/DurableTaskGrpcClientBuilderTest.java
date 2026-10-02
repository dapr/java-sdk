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

package io.dapr.workflows.task.client;

import io.dapr.workflows.task.serialization.JacksonDataConverter;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DurableTaskGrpcClientBuilderTest {

  private ManagedChannel channel;

  @AfterEach
  public void tearDown() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private ManagedChannel channel() {
    channel = InProcessChannelBuilder.forName(InProcessServerBuilder.generateName()).build();
    return channel;
  }

  @Test
  public void shouldReturnItselfFromEverySetterSoCallsCanChain() {
    DurableTaskGrpcClientBuilder builder = new DurableTaskGrpcClientBuilder();
    Tracer tracer = SdkTracerProvider.builder().build().get("test");

    assertSame(builder, builder.dataConverter(new JacksonDataConverter()));
    assertSame(builder, builder.tracer(tracer));
    assertSame(builder, builder.port(4001));
    assertSame(builder, builder.tlsCaPath("/tmp/ca.pem"));
    assertSame(builder, builder.tlsCertPath("/tmp/cert.pem"));
    assertSame(builder, builder.tlsKeyPath("/tmp/key.pem"));
    assertSame(builder, builder.insecure(true));
    assertSame(builder, builder.grpcChannel(channel()));
  }

  @Test
  public void shouldBuildAClientOverTheSuppliedChannel() throws Exception {
    try (DurableTaskClient client = new DurableTaskGrpcClientBuilder()
        .grpcChannel(channel())
        .dataConverter(new JacksonDataConverter())
        .build()) {
      assertNotNull(client);
    }
  }

  @Test
  public void shouldBuildAClientForAPortWithoutConnectingYet() throws Exception {
    // A channel is created lazily, so building against a port nothing listens on must not fail here.
    try (DurableTaskClient client = new DurableTaskGrpcClientBuilder().port(4001).build()) {
      assertNotNull(client);
    }
  }
}
