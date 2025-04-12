package io.dapr.springboot.examples.consumer.invoke.grpc;

import io.dapr.examples.DaprExamplesProtos;
import io.dapr.examples.HelloWorldGrpc;
import io.dapr.spring.invoke.grpc.client.DaprGrpcClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

  @DaprGrpcClient("producer-app")
  private HelloWorldGrpc.HelloWorldBlockingStub blockingStub;

  @RequestMapping("/hello")
  public String sayHello(@RequestParam("name") String name) {
    DaprExamplesProtos.HelloRequest request = DaprExamplesProtos.HelloRequest.newBuilder().setName(name).build();
    DaprExamplesProtos.HelloReply reply = blockingStub.sayHello(request);
    return reply.getMessage();
  }
}
