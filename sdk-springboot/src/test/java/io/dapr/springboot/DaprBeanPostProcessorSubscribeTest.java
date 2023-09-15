/*
 * Copyright 2023 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package io.dapr.springboot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class DaprBeanPostProcessorSubscribeTest {

  @Test
  public void testPostProcessBeforeInitialization() throws NoSuchMethodException {
    Method subscribeToTopicsMethod = DaprBeanPostProcessor.class.getDeclaredMethod(
            "subscribeToTopics", Class.class, StringValueResolver.class, DaprRuntime.class);
    subscribeToTopicsMethod.setAccessible(true);

    DaprRuntime runtime = getDaprRuntime();

    try {
      subscribeToTopicsMethod.invoke(DaprBeanPostProcessor.class, MockControllerWithSubscribe.class,
              new MockStringValueResolver(), runtime);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    DaprTopicSubscription[] topicSubscriptions = runtime.listSubscribedTopics();

    // There should be three subscriptions.
    Assertions.assertEquals(2, topicSubscriptions.length);

    DaprTopicSubscription[] expectedDaprTopicSubscriptions = getTestDaprTopicSubscriptions();

    // Subscription without BulkSubscribe.
    this.assertTopicSubscriptionEquality(expectedDaprTopicSubscriptions[0], topicSubscriptions[0]);

    // Subscription with BulkSubscribe.
    // This should correctly set the bulkSubscribe field.
    this.assertTopicSubscriptionEquality(expectedDaprTopicSubscriptions[1], topicSubscriptions[1]);
  }

  private void assertTopicSubscriptionEquality(DaprTopicSubscription s1, DaprTopicSubscription s2) {
    Assertions.assertEquals(s1.getPubsubName(), s2.getPubsubName());
    Assertions.assertEquals(s1.getTopic(), s2.getTopic());
    Assertions.assertEquals(s1.getRoute(), s2.getRoute());
    Assertions.assertEquals(s1.getMetadata(), s2.getMetadata());
    if (s1.getBulkSubscribe() == null) {
      Assertions.assertNull(s2.getBulkSubscribe());
    } else {
      Assertions.assertEquals(s1.getBulkSubscribe().isEnabled(), s2.getBulkSubscribe().isEnabled());
      Assertions.assertEquals(s1.getBulkSubscribe().getMaxAwaitDurationMs(), s2.getBulkSubscribe().getMaxAwaitDurationMs());
      Assertions.assertEquals(s1.getBulkSubscribe().getMaxMessagesCount(), s2.getBulkSubscribe().getMaxMessagesCount());
    }
  }

  private DaprTopicSubscription[] getTestDaprTopicSubscriptions() {
    DaprTopicSubscription[] daprTopicSubscriptions = new DaprTopicSubscription[3];
    daprTopicSubscriptions[0] = new DaprTopicSubscription(
            MockControllerWithSubscribe.pubSubName,
            MockControllerWithSubscribe.topicName,
            MockControllerWithSubscribe.subscribeRoute,
            MockControllerWithSubscribe.deadLetterTopic,
            new HashMap<>());

    DaprTopicBulkSubscribe bulkSubscribe = new DaprTopicBulkSubscribe(true);
    bulkSubscribe.setMaxMessagesCount(MockControllerWithSubscribe.maxMessagesCount);
    bulkSubscribe.setMaxAwaitDurationMs(MockControllerWithSubscribe.maxAwaitDurationMs);

    daprTopicSubscriptions[1] = new DaprTopicSubscription(
            MockControllerWithSubscribe.pubSubName,
            MockControllerWithSubscribe.bulkTopicName,
            MockControllerWithSubscribe.bulkSubscribeRoute,
            MockControllerWithSubscribe.deadLetterTopic,
            new HashMap<>(),
            bulkSubscribe);

    return daprTopicSubscriptions;
  }

  private DaprRuntime getDaprRuntime() {
    try {
      Constructor<DaprRuntime> constructor = DaprRuntime.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    }catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
