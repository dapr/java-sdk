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
