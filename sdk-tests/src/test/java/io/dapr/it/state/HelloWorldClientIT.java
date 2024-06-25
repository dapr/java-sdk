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

package io.dapr.it.state;

import io.dapr.config.Properties;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HelloWorldClientIT extends BaseIT {

  @Test
  public void testHelloWorldState() throws Exception {
    DaprRun daprRun = startDaprApp(
        HelloWorldClientIT.class.getSimpleName(),
        HelloWorldGrpcStateService.SUCCESS_MESSAGE,
        HelloWorldGrpcStateService.class,
        false,
        2000
    );
    ManagedChannel channel =
      ManagedChannelBuilder.forAddress(Properties.SIDECAR_IP.get(), daprRun.getGrpcPort()).usePlaintext().build();
    DaprGrpc.DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);

    String key = "mykey";
    {
      DaprProtos.GetStateRequest req = DaprProtos.GetStateRequest
        .newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build();
      DaprProtos.GetStateResponse response = client.getState(req);
      String value = response.getData().toStringUtf8();
      System.out.println("Got: " + value);
      Assertions.assertEquals("Hello World", value);
    }

    // Then, delete it.
    {
      DaprProtos.DeleteStateRequest req = DaprProtos.DeleteStateRequest
        .newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build();
      client.deleteState(req);
      System.out.println("Deleted!");
    }

    {
      DaprProtos.GetStateRequest req = DaprProtos.GetStateRequest
        .newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build();
      DaprProtos.GetStateResponse response = client.getState(req);
      String value = response.getData().toStringUtf8();
      System.out.println("Got: " + value);
      Assertions.assertEquals("", value);
    }
    channel.shutdown();
  }
}
