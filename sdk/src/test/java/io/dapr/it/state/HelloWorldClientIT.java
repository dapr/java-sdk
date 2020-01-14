/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprIntegrationTestingRunner;
import io.dapr.it.services.HelloWorldGrpcStateService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.dapr.it.DaprIntegrationTestingRunner.DAPR_FREEPORTS;

public class HelloWorldClientIT extends BaseIT {

  private static DaprIntegrationTestingRunner daprIntegrationTestingRunner;

  @BeforeClass
  public static void init() throws Exception {
    daprIntegrationTestingRunner =
      createDaprIntegrationTestingRunner(
        "BUILD SUCCESS",
        HelloWorldGrpcStateService.class,
        false,
        2000
      );
    daprIntegrationTestingRunner.initializeDapr();
  }

  @Test
  public void testHelloWorldState(){
    ManagedChannel channel =
      ManagedChannelBuilder.forAddress("localhost", DAPR_FREEPORTS.getGrpcPort()).usePlaintext().build();
    DaprGrpc.DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);

    String key = "mykey";
    {
      DaprProtos.GetStateEnvelope req = DaprProtos.GetStateEnvelope
        .newBuilder()
        .setKey(key)
        .build();
      DaprProtos.GetStateResponseEnvelope response = client.getState(req);
      String value = response.getData().getValue().toStringUtf8();
      System.out.println("Got: " + value);
      Assert.assertEquals("Hello World",value);
    }

    // Then, delete it.
    {
      DaprProtos.DeleteStateEnvelope req = DaprProtos.DeleteStateEnvelope
        .newBuilder()
        .setKey(key)
        .build();
      client.deleteState(req);
      System.out.println("Deleted!");
    }

    {
      DaprProtos.GetStateEnvelope req = DaprProtos.GetStateEnvelope
        .newBuilder()
        .setKey(key)
        .build();
      DaprProtos.GetStateResponseEnvelope response = client.getState(req);
      String value = response.getData().getValue().toStringUtf8();
      System.out.println("Got: " + value);
      Assert.assertEquals("",value);
    }
  }
}
