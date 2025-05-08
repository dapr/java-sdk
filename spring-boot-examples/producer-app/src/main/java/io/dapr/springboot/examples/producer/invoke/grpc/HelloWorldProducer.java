package io.dapr.springboot.examples.producer.invoke.grpc;

import io.dapr.springboot.examples.HelloWorldGrpc;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class HelloWorldProducer extends HelloWorldGrpc.HelloWorldImplBase {

  @Override
  public void sayHello(io.dapr.springboot.examples.DaprExamplesProtos.HelloRequest request,
                       io.grpc.stub.StreamObserver<io.dapr.springboot.examples.DaprExamplesProtos.HelloReply> responseObserver) {
    io.dapr.springboot.examples.DaprExamplesProtos.HelloReply reply = io.dapr.springboot.examples.DaprExamplesProtos.HelloReply.newBuilder()
        .setMessage("Hello " + request.getName())
        .build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
