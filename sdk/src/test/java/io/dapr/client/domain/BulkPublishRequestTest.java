/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client.domain;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNull;

public class BulkPublishRequestTest {

  @Test
  public void testSetMetadata() {
    BulkPublishRequest<String> request = new BulkPublishRequest<>("testPubsub", "testTopic");
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    Map<String, String> initial = request.getMetadata();
    request.setMetadata(metadata);
    Assert.assertNotSame("Should not be same map", request.getMetadata(), initial);
  }

  @Test
  public void testSetEntries() {
    BulkPublishRequest<String> request = new BulkPublishRequest<>("testPubsub", "testTopic");
    // Null check
    request.setEntries(null);
    assertNull(request.getEntries());
    // Modifiability check
    BulkPublishEntry<String> testEntry = new BulkPublishEntry<>("1", "test event", "text/plain", null);
    List<BulkPublishEntry<String>> entryList = new ArrayList<>();
    entryList.add(testEntry);
    request.setEntries(entryList);
    List<BulkPublishEntry<String>> initial = request.getEntries();
    request.setEntries(entryList);
    Assert.assertNotSame("Should not be same map", request.getEntries(), initial);
  }
}
