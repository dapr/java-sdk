package io.dapr.it.spring.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRestController {

  @GetMapping("/hello")
  public String hello() {
    return "hello";
  }

  @PostMapping("/echo")
  public String echo(@RequestBody String input) {
    return input;
  }

  @PostMapping("/echoj")
  public Result echoJson(@RequestBody String input) {
    return new Result(input);
  }
}
