package io.dapr.springboot;

import io.dapr.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DaprRuntimeTest {

  @Test
  public void testPubsubDefaultPathDuplicateRegistration() {
    String pubSubName = "pubsub";
    String topicName = "topic";
    String deadLetterTopic = "deadLetterTopic";
    String match = "";
    String route = String.format("%s/%s", pubSubName, topicName);
    HashMap<String, String> metadata = new HashMap<String, String>();

    Rule rule = new Rule(){
      @Override
      public Class<? extends Annotation> annotationType() {
        return Rule.class;
      }

      public String match() {
        return match;
      }
      public int priority() {
        return 0;
      }
    };

    DaprRuntime runtime = DaprRuntime.getInstance();
    Assertions.assertNotNull(runtime);

    // We should be able to register the same route multiple times
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), route,deadLetterTopic, metadata);
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), route,deadLetterTopic, metadata);
  }

  @Test
  public void testPubsubDefaultPathDifferentRegistration() {
    String pubSubName = "pubsub";
    String topicName = "topic";
    String deadLetterTopic = "deadLetterTopic";
    String match = "";
    String firstRoute = String.format("%s/%s", pubSubName, topicName);
    String secondRoute = String.format("%s/%s/subscribe", pubSubName, topicName);


    HashMap<String, String> metadata = new HashMap<String, String>();

    Rule rule = new Rule(){
      @Override
      public Class<? extends Annotation> annotationType() {
        return Rule.class;
      }

      public String match() {
        return match;
      }
      public int priority() {
        return 0;
      }
    };

    DaprRuntime runtime = DaprRuntime.getInstance();

    Assertions.assertNotNull(runtime);
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), firstRoute, deadLetterTopic, metadata);

    // Supplying the same pubsub bits but a different route should fail
    assertThrows(RuntimeException.class, () -> runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), secondRoute, deadLetterTopic, metadata));
  }

}
