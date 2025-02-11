package io.dapr.springboot.examples.producer;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TestSubscriberRestController {

  private List<CloudEvent> events = new ArrayList<>();

  private final Logger logger = LoggerFactory.getLogger(TestSubscriberRestController.class);

  @PostMapping("subscribe")
  @Topic(pubsubName = "pubsub", name = "topic")
  public void subscribe(@RequestBody CloudEvent<Order> cloudEvent){
    logger.info("Order Event Received: " + cloudEvent.getData());
    events.add(cloudEvent);
  }

  public List<CloudEvent> getAllEvents() {
    return events;
  }
}

