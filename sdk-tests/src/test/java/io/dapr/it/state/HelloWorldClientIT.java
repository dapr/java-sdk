/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.junit.Test;

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
      ManagedChannelBuilder.forAddress("localhost", daprRun.getGrpcPort()).usePlaintext().build();
    DaprGrpc.DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);

    String key = "mykey";
    {
      DaprProtos.GetStateEnvelope req = DaprProtos.GetStateEnvelope
        .newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build();
      DaprProtos.GetStateResponseEnvelope response = client.getState(req);
      String value = response.getData().getValue().toStringUtf8();
      System.out.println("Got: " + value);
      Assert.assertEquals("Hello World", value);
    }

    // Then, delete it.
    {
      DaprProtos.DeleteStateEnvelope req = DaprProtos.DeleteStateEnvelope
        .newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build();
      client.deleteState(req);
      System.out.println("Deleted!");
    }

    {
      DaprProtos.GetStateEnvelope req = DaprProtos.GetStateEnvelope
        .newBuilder()
        .setStoreName(STATE_STORE_NAME)
        .setKey(key)
        .build();
      DaprProtos.GetStateResponseEnvelope response = client.getState(req);
      String value = response.getData().getValue().toStringUtf8();
      System.out.println("Got: " + value);
      Assert.assertEquals("", value);
    }
  }
}
