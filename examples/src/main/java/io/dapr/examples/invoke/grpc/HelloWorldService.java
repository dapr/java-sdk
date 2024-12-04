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

import io.dapr.examples.DaprExamplesProtos.HelloReply;
import io.dapr.examples.DaprExamplesProtos.HelloRequest;
import io.dapr.examples.HelloWorldGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run in server mode:
 * dapr run --app-id hellogrpc --app-port 5000 --app-protocol grpc \
 * -- java -jar target/dapr-java-sdk-examples-exec.jar
 * io.dapr.examples.invoke.grpc.HelloWorldService -p 5000
 */
public class HelloWorldService {
  private static final Logger logger = Logger.getLogger(HelloWorldService.class.getName());

  /**
   * Server mode: Grpc server.
   */
  private Server server;

  /**
   * Server mode: class that encapsulates server-side handling logic for Grpc.
   */
  static class HelloWorldImpl extends HelloWorldGrpc.HelloWorldImplBase {

    /**
     * Handling of the 'sayHello' method.
     *
     * @param req Request to say something.
     * @return Response with when it was said.
     */
    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      logger.info("greet to " + req.getName());
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
  
  /**
   * Server mode: starts listening on given port.
   *
   * @param port Port to listen on.
   * @throws IOException Errors while trying to start service.
   */
  private void start(int port) throws IOException {
    server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
        .addService(new HelloWorldImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown
        // hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          HelloWorldService.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  /**
   * Server mode: waits for shutdown trigger.
   *
   * @throws InterruptedException Propagated interrupted exception.
   */
  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon
   * threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * This is the main method of this app.
   * 
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

    final HelloWorldService service = new HelloWorldService();
    service.start(port);
    service.blockUntilShutdown();
  }

}
