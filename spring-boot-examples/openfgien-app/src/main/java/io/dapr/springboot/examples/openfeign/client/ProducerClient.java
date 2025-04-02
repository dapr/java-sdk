package io.dapr.springboot.examples.openfeign.client;

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import io.dapr.springboot.examples.openfeign.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "producer-client", url = "http://method.producer-app/")
@UseDaprClient
public interface ProducerClient {

  @PostMapping("/orders")
  String storeOrder(@RequestBody Order order);

  @GetMapping(value = "/orders", produces = "application/json")
  Iterable<Order> getAll();

  @GetMapping(value = "/orders/byItem/", produces = "application/json")
  Iterable<Order> getAllByItem(@RequestParam("item") String item);
}
