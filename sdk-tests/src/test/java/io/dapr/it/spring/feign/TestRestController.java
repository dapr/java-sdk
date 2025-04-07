package io.dapr.it.spring.feign;

import io.dapr.Topic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRestController {

  public static final String pubSubName = "pubsub";
  public static final String topicName = "mockTopic";

  @GetMapping("/ready")
  public String ok() {
    return "OK";
  }

  @GetMapping("/hello")
  public String hello() {
    return "hello";
  }

  @Topic(name = topicName, pubsubName = pubSubName)
  @PostMapping("/echo")
  public String echo(@RequestBody String input) {
    return input;
  }

  @PostMapping("/echoj")
  public Result echoJson(@RequestBody String input) {
    return new Result(input);
  }
}
