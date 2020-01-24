package io.dapr.examples.invoke.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos.InvokeServiceEnvelope;
import io.dapr.DaprProtos.InvokeServiceResponseEnvelope;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.dapr.examples.DaprExamplesProtos.SayRequest;
import static io.dapr.examples.DaprExamplesProtos.SayResponse;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. Send messages to the server:
 * dapr run --protocol grpc --grpc-port 50001 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.invoke.grpc.HelloWorldClient -Dexec.args="-p 50001 'message one' 'message two'"
 */
public class HelloWorldClient {

  /**
   * Client mode: class representing a client-side logic for calling HelloWorld over Dapr.
   */
  private static class GrpcHelloWorldDaprClient {

    /**
     * Client communication channel: host, port and tls(on/off)
     */
    private final ManagedChannel channel;

    /**
     * Calls will be done asynchronously.
     */
    private final DaprGrpc.DaprFutureStub client;

    /**
     * Creates a Grpc client for the DaprGrpc service.
     *
     * @param host host for the remote service endpoint
     * @param port port for the remote service endpoint
     */
    public GrpcHelloWorldDaprClient(String host, int port) {
      this(ManagedChannelBuilder
        .forAddress("localhost", port)
        .usePlaintext()  // SSL/TLS is default, we turn it off just because this is a sample and not prod.
        .build());
    }

    /**
     * Helper constructor to build client from channel.
     *
     * @param channel
     */
    private GrpcHelloWorldDaprClient(ManagedChannel channel) {
      this.channel = channel;
      this.client = DaprGrpc.newFutureStub(channel);
    }

    /**
     * Client mode: sends messages, one per second.
     *
     * @param messages
     */
    private void sendMessages(String... messages) throws ExecutionException, InterruptedException, InvalidProtocolBufferException {
      List<ListenableFuture<InvokeServiceResponseEnvelope>> futureResponses = new ArrayList<>();
      for (String message : messages) {
        SayRequest request = SayRequest
          .newBuilder()
          .setMessage(message)
          .build();

        // Now, wrap the request with Dapr's envelope.
        InvokeServiceEnvelope requestEnvelope = InvokeServiceEnvelope
          .newBuilder()
          .setId("hellogrpc")  // Service's identifier.
          .setData(Any.pack(request))
          .setMethod("say")  // The service's method to be invoked by Dapr.
          .build();

        futureResponses.add(client.invokeService(requestEnvelope));
        System.out.println("Client: sent => " + message);
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      }

      for (ListenableFuture<InvokeServiceResponseEnvelope> future : futureResponses) {
        Any data = future.get().getData();  // Blocks waiting for response.
        // IMPORTANT: do not use Any.unpack(), use Type.ParseFrom() instead.
        SayResponse response = SayResponse.parseFrom(data.getValue());
        System.out.println("Client: got response => " + response.getTimestamp());
      }
    }

    /**
     * Client mode: gracefully shutdown client within 1 min, otherwise force it.
     *
     * @throws InterruptedException Propagated interrupted exception.
     */
    private void shutdown() throws InterruptedException {
      this.channel.shutdown().awaitTermination(1, TimeUnit.MINUTES);
      System.out.println("Client: Bye.");
    }

  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port to listen or send event to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    GrpcHelloWorldDaprClient helloWorldClient = new GrpcHelloWorldDaprClient("localhost", port);
    helloWorldClient.sendMessages(cmd.getArgs());
    helloWorldClient.shutdown();
  }
}
