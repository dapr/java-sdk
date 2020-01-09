package io.dapr.it.state;

import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.it.DaprIntegrationTestingRunner;
import io.dapr.it.services.HelloWorldGrpcStateService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.*;


import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class HelloWorldClientIT {


    private static DaprIntegrationTestingRunner daprIntegrationTestingRunner;

    @BeforeClass
    public static void init() throws IOException, InterruptedException, TimeoutException, ExecutionException {
         daprIntegrationTestingRunner =
                DaprIntegrationTestingRunner.createDaprIntegrationTestingRunner(
                        "BUILD SUCCESS",
                        HelloWorldGrpcStateService.class,
                        true,
                        50001,
                        2000
                        );
        daprIntegrationTestingRunner.initializeDapr();
    }

    @Test
    public void testHelloWorldState(){
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("localhost", 50001).usePlaintext().build();
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

    @Test
    public void test2(){

    }

    @AfterClass
    public static void cleanUp() throws IOException {
        daprIntegrationTestingRunner.destroyDapr();

    }

}
