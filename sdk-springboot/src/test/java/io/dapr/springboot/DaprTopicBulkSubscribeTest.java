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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DaprTopicBulkSubscribeTest {
  @Test
  public void testGettersAndSetters() {
    DaprTopicBulkSubscribe bulkSubscribe = new DaprTopicBulkSubscribe(true);
    bulkSubscribe.setMaxMessagesCount(100);
    bulkSubscribe.setMaxAwaitDurationMs(200);

    Assert.assertTrue(bulkSubscribe.isEnabled());
    Assert.assertEquals(100, bulkSubscribe.getMaxMessagesCount().longValue());
    Assert.assertEquals(200, bulkSubscribe.getMaxAwaitDurationMs().longValue());

    bulkSubscribe.setEnabled(false);
    Assert.assertFalse(bulkSubscribe.isEnabled());
  }

  @Test
  public void testSetMaxMessagesCount() {
    DaprTopicBulkSubscribe bulkSubscribe = new DaprTopicBulkSubscribe(true);

    // "value to be put" vs "should throw exception"
    Map<Integer, Boolean> testCases = new HashMap<Integer, Boolean>() {{
      put(-1, true);
      put(0, true);
      put(1, false);
    }};

    for (Map.Entry<Integer, Boolean> testCase: testCases.entrySet()) {
      try {
        bulkSubscribe.setMaxMessagesCount(testCase.getKey());
        Assert.assertFalse(testCase.getValue());
      } catch (IllegalArgumentException e) {
        Assert.assertTrue(testCase.getValue());
      }
    }
  }

  @Test
  public void testSetMaxAwaitDurationMs() {
    DaprTopicBulkSubscribe bulkSubscribe = new DaprTopicBulkSubscribe(true);

    // "value to be put" vs "should throw exception"
    Map<Integer, Boolean> testCases = new HashMap<Integer, Boolean>() {{
      put(-1, true);
      put(0, false);
      put(1, false);
    }};

    for (Map.Entry<Integer, Boolean> testCase: testCases.entrySet()) {
      try {
        bulkSubscribe.setMaxAwaitDurationMs(testCase.getKey());
        Assert.assertFalse(testCase.getValue());
      } catch (IllegalArgumentException e) {
        Assert.assertTrue(testCase.getValue());
      }
    }
  }
}
