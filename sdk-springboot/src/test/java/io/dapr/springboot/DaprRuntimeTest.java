package io.dapr.springboot;

import io.dapr.Rule;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.HashMap;

public class DaprRuntimeTest {

  @Test
  public void testPubsubDefaultPathDuplicateRegistration() {
    String pubSubName = "pubsub";
    String topicName = "topic";
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
    Assert.assertNotNull(runtime);

    // We should be able to register the same route multiple times
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), route, metadata);
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), route, metadata);
  }

  @Test(expected = RuntimeException.class)
  public void testPubsubDefaultPathDifferentRegistration() {
    String pubSubName = "pubsub";
    String topicName = "topic";
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

    Assert.assertNotNull(runtime);
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), firstRoute, metadata);

    // Supplying the same pubsub bits but a different route should fail
    runtime.addSubscribedTopic(
            pubSubName, topicName, match, rule.priority(), secondRoute, metadata);

  }

}
