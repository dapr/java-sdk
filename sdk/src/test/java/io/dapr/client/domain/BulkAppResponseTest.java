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
limitations under the License.
*/

package io.dapr.client.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BulkAppResponseTest {
  private BulkAppResponse getBulkAppResponse(List<BulkAppResponseEntry> statuses, boolean useCtr) {
    if (useCtr) {
      return new BulkAppResponse(statuses);
    } else {
      BulkAppResponse response = new BulkAppResponse();
      response.setStatuses(statuses);
      return response;
    }
  }

  @Test
  public void testSetStatus() {
    for (boolean b: new boolean[]{true, false}) {
      // Scenario 1: When status is null
      BulkAppResponse responseWithNull = getBulkAppResponse(null, b);
      assertNull(responseWithNull.getStatuses());

      // Scenario 2: When status has items
      BulkAppResponseEntry status1 = new BulkAppResponseEntry("1", BulkAppResponseStatus.SUCCESS);
      BulkAppResponseEntry status2 = new BulkAppResponseEntry("2", BulkAppResponseStatus.SUCCESS);
      List<BulkAppResponseEntry> statuses = new ArrayList<>(Arrays.asList(status1, status2));
      BulkAppResponse response = getBulkAppResponse(statuses, b);

      List<BulkAppResponseEntry> statusesFromGet = response.getStatuses();
      assertEquals(2, statusesFromGet.size());
      assertEquals("1", statusesFromGet.get(0).getEntryID());
      assertEquals("2", statusesFromGet.get(1).getEntryID());
    }
  }
}
