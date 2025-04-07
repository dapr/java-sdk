package io.dapr.it.spring.feign;

import io.dapr.Topic;
import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "test-method", url = "http://method.dapr-feign-test/")
@UseDaprClient
public interface TestMethodClient {

  @GetMapping(value = "/hello")
  String hello();

  @PostMapping("/echo")
  String echo(@RequestBody String input);

  @PostMapping(value = "/echoj", produces = "application/json;charset=utf-8")
  Result echoJson(@RequestBody String input);

}
