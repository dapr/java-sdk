/*
 * Copyright 2022 The Dapr Authors
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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class DaprBeanPostProcessorSubscribeTest {

  @Test
  public void testPostProcessBeforeInitialization() throws NoSuchMethodException {
    Method subscribeToTopicsMethod = DaprBeanPostProcessor.class.getDeclaredMethod(
            "subscribeToTopics", Class.class, StringValueResolver.class, DaprRuntime.class);
    subscribeToTopicsMethod.setAccessible(true);

    DaprTopicSubscription normalTopicSubscription = new DaprTopicSubscription(
            MockControllerWithSubscribe.pubSubName,
            MockControllerWithSubscribe.topicName,
            MockControllerWithSubscribe.subscribeRoute,
            new HashMap<>());

    HashMap<String, String> bulkTopicSubscriptionMetadata = new HashMap<String, String>() {{
      put(DaprBeanPostProcessor.BULK_SUBSCRIBE_METADATA_KEY, "true");
      put(DaprBeanPostProcessor.BULK_SUBSCRIBE_METADATA_MAX_COUNT_KEY,
              String.valueOf(MockControllerWithSubscribe.maxBulkSubCount));
      put(DaprBeanPostProcessor.BULK_SUBSCRIBE_METADATA_MAX_AWAIT_DURATION_MS_KEY,
              String.valueOf(MockControllerWithSubscribe.maxBulkSubAwaitDurationMs));
    }};
    DaprTopicSubscription bulkTopicSubscription = new DaprTopicSubscription(
            MockControllerWithSubscribe.pubSubName,
            MockControllerWithSubscribe.bulkTopicName,
            MockControllerWithSubscribe.bulkSubscribeRoute,
            bulkTopicSubscriptionMetadata);

    DaprRuntime runtime = new DaprRuntime();

    try {
      subscribeToTopicsMethod.invoke(DaprBeanPostProcessor.class, MockControllerWithSubscribe.class,
              new MockStringValueResolver(), runtime);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    DaprTopicSubscription[] topicSubscriptions = runtime.listSubscribedTopics();
    Assert.assertEquals(2, topicSubscriptions.length);

    this.assertTopicSubscriptionEquality(normalTopicSubscription, topicSubscriptions[0]);
    this.assertTopicSubscriptionEquality(bulkTopicSubscription, topicSubscriptions[1]);
  }

  private void assertTopicSubscriptionEquality(DaprTopicSubscription s1, DaprTopicSubscription s2) {
    Assert.assertEquals(s1.getPubsubName(), s2.getPubsubName());
    Assert.assertEquals(s1.getTopic(), s2.getTopic());
    Assert.assertEquals(s1.getRoute(), s2.getRoute());
    Assert.assertEquals(s1.getMetadata(), s2.getMetadata());
  }
}
