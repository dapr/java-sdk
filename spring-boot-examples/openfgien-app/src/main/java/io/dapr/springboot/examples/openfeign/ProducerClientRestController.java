package io.dapr.springboot.examples.openfeign;

import io.dapr.springboot.examples.openfeign.client.ProducerClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rpc/producer")
public class ProducerClientRestController {
  private final ProducerClient producerClient;

  public ProducerClientRestController(ProducerClient producerClient) {
    this.producerClient = producerClient;
  }

  @PostMapping("/orders")
  public String storeOrder(@RequestBody Order order) {
    return producerClient.storeOrder(order);
  }

  @GetMapping("/orders")
  public Iterable<Order> getAll() {
    return producerClient.getAll();
  }

  @GetMapping("/orders/byItem/")
  public Iterable<Order> getAllByItem(@RequestParam("item") String item) {
    return producerClient.getAllByItem(item);
  }
}
