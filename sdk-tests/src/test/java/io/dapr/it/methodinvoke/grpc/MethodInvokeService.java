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

import com.google.protobuf.Any;
import io.dapr.grpc.GrpcHealthCheckService;
import io.dapr.v1.AppCallbackGrpc;
import io.dapr.v1.CommonProtos;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.dapr.it.MethodInvokeServiceProtos.DeleteMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.DeleteMessageResponse;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesRequest;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesResponse;
import static io.dapr.it.MethodInvokeServiceProtos.PostMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.PostMessageResponse;
import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static io.dapr.it.MethodInvokeServiceProtos.SleepResponse;

public class MethodInvokeService {

  public static final String SUCCESS_MESSAGE = "application discovered on port ";

  /**
   * Server mode: class that encapsulates all server-side logic for Grpc.
   */
  private static class MyDaprService extends AppCallbackGrpc.AppCallbackImplBase {

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
     * Server mode: this is the Dapr method to receive Invoke operations via Grpc.
     *
     * @param request          Dapr envelope request,
     * @param responseObserver Dapr envelope response.
     */
    @Override
    public void onInvoke(CommonProtos.InvokeRequest request,
                         StreamObserver<CommonProtos.InvokeResponse> responseObserver) {
      try {
        if ("postMessage".equals(request.getMethod())) {
          PostMessageRequest req = PostMessageRequest.parseFrom(request.getData().getValue().toByteArray());

          this.messages.put(req.getId(), req.getMessage());

          CommonProtos.InvokeResponse.Builder responseBuilder = CommonProtos.InvokeResponse.newBuilder();
          responseBuilder.setData(Any.pack(PostMessageResponse.newBuilder().build()));
          responseObserver.onNext(responseBuilder.build());
        }
        if ("deleteMessage".equals(request.getMethod())) {
          DeleteMessageRequest req = DeleteMessageRequest.parseFrom(request.getData().getValue().toByteArray());

          this.messages.remove(req.getId());

          CommonProtos.InvokeResponse.Builder responseBuilder = CommonProtos.InvokeResponse.newBuilder();
          responseBuilder.setData(Any.pack(DeleteMessageResponse.newBuilder().build()));
          responseObserver.onNext(responseBuilder.build());
        }
        if ("getMessages".equals(request.getMethod())) {
          GetMessagesRequest.parseFrom(request.getData().getValue().toByteArray());

          GetMessagesResponse res = GetMessagesResponse.newBuilder().putAllMessages(this.messages).build();

          CommonProtos.InvokeResponse.Builder responseBuilder = CommonProtos.InvokeResponse.newBuilder();
          responseBuilder.setData(Any.pack(res));
          responseObserver.onNext(responseBuilder.build());
        }
        if ("sleep".equals(request.getMethod())) {
          SleepRequest req = SleepRequest.parseFrom(request.getData().getValue().toByteArray());

          SleepResponse res = this.sleep(req);

          CommonProtos.InvokeResponse.Builder responseBuilder = CommonProtos.InvokeResponse.newBuilder();
          responseBuilder.setData(Any.pack(res));
          responseObserver.onNext(responseBuilder.build());
        }
      } catch (Exception e) {
        responseObserver.onError(e);
      } finally {
        responseObserver.onCompleted();
      }
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

    System.out.printf("Service starting on port %d ...\n", port);

    final MyDaprService service = new MyDaprService();
    service.start(port);
    service.awaitTermination();
  }
}
