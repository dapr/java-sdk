/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
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
      ManagedChannelBuilder.forAddress("127.0.0.1", daprRun.getGrpcPort()).usePlaintext().build();
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
      Assert.assertEquals("Hello World", value);
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
      Assert.assertEquals("", value);
    }
    channel.shutdown();
  }
}
