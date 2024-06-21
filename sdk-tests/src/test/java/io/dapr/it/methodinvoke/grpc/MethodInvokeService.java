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

package io.dapr.it.methodinvoke.grpc;

import io.dapr.grpc.GrpcHealthCheckService;
import io.dapr.it.DaprRunConfig;
import io.dapr.it.MethodInvokeServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesResponse;
import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static io.dapr.it.MethodInvokeServiceProtos.SleepResponse;

@DaprRunConfig(
        enableAppHealthCheck = true
)
public class MethodInvokeService {

  private static final long STARTUP_DELAY_SECONDS = 10;

  public static final String SUCCESS_MESSAGE = "application discovered on port ";

  /**
   * Server mode: class that encapsulates all server-side logic for Grpc.
   */
  private static class MyDaprService extends MethodInvokeServiceGrpc.MethodInvokeServiceImplBase {

    private final Map<Integer, String> messages = Collections.synchronizedMap(new HashMap<>());

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
          .addService(new GrpcHealthCheckService())
          .build()
          .start();
      System.out.printf("Server: started listening on port %d\n", port);

      // Now we handle ctrl+c (or any other JVM shutdown)
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Server: shutting down gracefully ...");
        MyDaprService.this.server.shutdown();
        System.out.println("Server: Bye.");
      }));
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
     * {@inheritDoc}
     */
    public void postMessage(io.dapr.it.MethodInvokeServiceProtos.PostMessageRequest request,
                            io.grpc.stub.StreamObserver<io.dapr.it.MethodInvokeServiceProtos.PostMessageResponse> responseObserver) {
      this.messages.put(request.getId(), request.getMessage());

      io.dapr.it.MethodInvokeServiceProtos.PostMessageResponse.Builder responseBuilder =
          io.dapr.it.MethodInvokeServiceProtos.PostMessageResponse.newBuilder();
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    }

    /**
     * {@inheritDoc}
     */
    public void deleteMessage(io.dapr.it.MethodInvokeServiceProtos.DeleteMessageRequest request,
                              io.grpc.stub.StreamObserver<io.dapr.it.MethodInvokeServiceProtos.DeleteMessageResponse> responseObserver) {
      this.messages.remove(request.getId());

      io.dapr.it.MethodInvokeServiceProtos.DeleteMessageResponse.Builder responseBuilder =
          io.dapr.it.MethodInvokeServiceProtos.DeleteMessageResponse.newBuilder();
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    }

    /**
     * {@inheritDoc}
     */
    public void getMessages(io.dapr.it.MethodInvokeServiceProtos.GetMessagesRequest request,
                            io.grpc.stub.StreamObserver<io.dapr.it.MethodInvokeServiceProtos.GetMessagesResponse> responseObserver) {
      GetMessagesResponse res = GetMessagesResponse.newBuilder().putAllMessages(this.messages).build();

      io.dapr.it.MethodInvokeServiceProtos.GetMessagesResponse.Builder responseBuilder
          = io.dapr.it.MethodInvokeServiceProtos.GetMessagesResponse.newBuilder();
      responseObserver.onNext(res);
      responseObserver.onCompleted();
    }

    /**
     * {@inheritDoc}
     */
    public void sleep(io.dapr.it.MethodInvokeServiceProtos.SleepRequest request,
                      io.grpc.stub.StreamObserver<io.dapr.it.MethodInvokeServiceProtos.SleepResponse> responseObserver) {
      SleepResponse res = this.sleep(request);

      io.dapr.it.MethodInvokeServiceProtos.SleepResponse.Builder responseBuilder =
          io.dapr.it.MethodInvokeServiceProtos.SleepResponse.newBuilder();
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    }

    public SleepResponse sleep(SleepRequest request) {
      if (request.getSeconds() < 0) {
        throw new IllegalArgumentException("Sleep time cannot be negative.");
      }

      try {
        Thread.sleep(request.getSeconds() * 1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        new RuntimeException(e);
      }

      // Now respond with current timestamp.
      return SleepResponse.newBuilder().build();
    }
  }

  /**
   * This is the main method of this app.
   * @param args The port to listen on.
   * @throws Exception An Exception.
   */
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(args[0]);

    System.out.printf("Service to start on port %d ...\n", port);

    // The artificial delay is useful to detect bugs in app health, where the app is invoked too soon.
    System.out.printf("Artificial delay of %d seconds ...\n", STARTUP_DELAY_SECONDS);
    Thread.sleep(STARTUP_DELAY_SECONDS * 1000);
    System.out.printf("Now starting ...\n", STARTUP_DELAY_SECONDS);
    final MyDaprService service = new MyDaprService();
    service.start(port);
    service.awaitTermination();
  }
}
