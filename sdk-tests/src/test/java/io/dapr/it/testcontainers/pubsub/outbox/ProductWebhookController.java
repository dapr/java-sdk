package io.dapr.it.testcontainers.pubsub.outbox;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/webhooks/products")
public class ProductWebhookController {

  public static final List<CloudEvent<Product>> EVENT_LIST = new ArrayList<>();

  @PostMapping("/created")
  @Topic(name = "product.created", pubsubName = "pubsub")
  public void handleProductCreated(@RequestBody CloudEvent cloudEvent) {
    System.out.println("Received product.created event: " + cloudEvent.getData());
    EVENT_LIST.add(cloudEvent);
  }
}
