package io.dapr.springboot.examples.producer;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TestSubscriberRestController {

  private List<CloudEvent> events = new ArrayList<>();


  @PostMapping("subscribe")
  @Topic(pubsubName = "pubsub", name = "topic")
  public void subscribe(@RequestBody CloudEvent<Order> cloudEvent){
    System.out.println("CONSUME +++++ " + cloudEvent);
    System.out.println("ORDER +++++ " + cloudEvent.getData());
    events.add(cloudEvent);
  }

  public List<CloudEvent> getAllEvents() {
    return events;
  }
}

