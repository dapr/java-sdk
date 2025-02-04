package io.dapr.springboot.examples.consumer;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SubscriberRestController {

  private List<CloudEvent> events = new ArrayList<>();

  /**
   * Subscribe to cloud events.
   * @param cloudEvent payload
   */
  @PostMapping("subscribe")
  @Topic(pubsubName = "pubsub", name = "topic")
  public void subscribe(@RequestBody CloudEvent<Order> cloudEvent) {
    System.out.println("CONSUME +++++ " + cloudEvent);
    System.out.println("ORDER +++++ " + cloudEvent.getData());
    events.add(cloudEvent);
  }

  @GetMapping("events")
  public List<CloudEvent> getAllEvents() {
    return events;
  }

}

