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

package io.dapr.examples.invoke.grpc;

import com.google.protobuf.Any;
import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.CommonProtos;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static io.dapr.examples.DaprExamplesProtos.SayRequest;
import static io.dapr.examples.DaprExamplesProtos.SayResponse;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run in server mode:
 * dapr run --app-id hellogrpc --app-port 5000 --app-protocol grpc \
 *   -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.grpc.HelloWorldService -p 5000
 */
public class HelloWorldService {

  /**
   * Server mode: class that encapsulates all server-side logic for Grpc.
   */
  private static class GrpcHelloWorldDaprService extends AppCallbackGrpc.AppCallbackImplBase {

    /**
     * Format to output date and time.
     */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Server mode: Grpc server.
     */
    private Server server;

    /**
     * Server mode: starts listening on given port.
     *
     * @param port Port to listen on.
     * @throws IOException Errors while trying to start service.
     */
    private void start(int port) throws IOException {
      this.server = ServerBuilder
          .forPort(port)
          .addService(this)
          .build()
          .start();
      System.out.printf("Server: started listening on port %d\n", port);

      // Now we handle ctrl+c (or any other JVM shutdown)
      Runtime.getRuntime().addShutdownHook(new Thread() {

        @Override
        public void run() {
          System.out.println("Server: shutting down gracefully ...");
          GrpcHelloWorldDaprService.this.server.shutdown();
          System.out.println("Server: Bye.");
        }
      });
    }

    /**
     * Server mode: waits for shutdown trigger.
     *
     * @throws InterruptedException Propagated interrupted exception.
     */
    private void awaitTermination() throws InterruptedException {
      if (this.server != null) {
        this.server.awaitTermination();
      }
    }

    /**
     * Server mode: this is the Dapr method to receive Invoke operations via Grpc.
     *
     * @param request          Dapr envelope request,
     * @param responseObserver Dapr envelope response.
     */
    @Override
    public void onInvoke(CommonProtos.InvokeRequest request,
                         StreamObserver<CommonProtos.InvokeResponse> responseObserver) {
      try {
        if ("say".equals(request.getMethod())) {
          SayRequest sayRequest =
              SayRequest.newBuilder().setMessage(request.getData().getValue().toStringUtf8()).build();
          SayResponse sayResponse = this.say(sayRequest);
          CommonProtos.InvokeResponse.Builder responseBuilder = CommonProtos.InvokeResponse.newBuilder();
          responseBuilder.setData(Any.pack(sayResponse));
          responseObserver.onNext(responseBuilder.build());
        }
      } finally {
        responseObserver.onCompleted();
      }
    }

    /**
     * Handling of the 'say' method.
     *
     * @param request Request to say something.
     * @return Response with when it was said.
     */
    public SayResponse say(SayRequest request) {
      Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      String utcNowAsString = DATE_FORMAT.format(utcNow.getTime());

      // Handles the request by printing message.
      System.out.println("Server: " + request.getMessage());
      System.out.println("@ " + utcNowAsString);

      // Now respond with current timestamp.
      SayResponse.Builder responseBuilder = SayResponse.newBuilder();
      return responseBuilder.setTimestamp(utcNowAsString).build();
    }
  }

  /**
   * This is the main method of this app.
   * @param args The port to listen on.
   * @throws Exception An Exception.
   */
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption("p", "port", true, "Port to listen to.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    final GrpcHelloWorldDaprService service = new GrpcHelloWorldDaprService();
    service.start(port);
    service.awaitTermination();
  }
}
