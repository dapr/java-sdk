package io.dapr.springboot.examples.producer.invoke.grpc;

import io.dapr.examples.HelloWorldGrpc;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

@GrpcService
public class HelloWorldProducer extends HelloWorldGrpc.HelloWorldImplBase {

  @Override
  public void sayHello(io.dapr.examples.DaprExamplesProtos.HelloRequest request,
                       io.grpc.stub.StreamObserver<io.dapr.examples.DaprExamplesProtos.HelloReply> responseObserver) {
    io.dapr.examples.DaprExamplesProtos.HelloReply reply = io.dapr.examples.DaprExamplesProtos.HelloReply.newBuilder()
        .setMessage("Hello " + request.getName())
        .build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }
}
