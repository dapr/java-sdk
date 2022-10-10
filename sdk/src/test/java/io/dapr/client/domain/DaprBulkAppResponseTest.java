package io.dapr.client.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DaprBulkAppResponseTest {
  private DaprBulkAppResponse getDaprBulkAppResponse(List<DaprBulkAppResponseEntry> statuses, boolean useCtr) {
    if (useCtr) {
      return new DaprBulkAppResponse(statuses);
    } else {
      DaprBulkAppResponse response = new DaprBulkAppResponse();
      response.setStatuses(statuses);
      return response;
    }
  }

  @Test
  public void testSetStatus() {
    for (boolean b: new boolean[]{true, false}) {
      // Scenario 1: When status is null
      DaprBulkAppResponse responseWithNull = getDaprBulkAppResponse(null, b);
      assertNull(responseWithNull.getStatuses());

      // Scenario 2: When status has items
      DaprBulkAppResponseEntry status1 = new DaprBulkAppResponseEntry("1", DaprBulkAppResponseStatus.SUCCESS);
      DaprBulkAppResponseEntry status2 = new DaprBulkAppResponseEntry("2", DaprBulkAppResponseStatus.SUCCESS);
      List<DaprBulkAppResponseEntry> statuses = new ArrayList<>(Arrays.asList(status1, status2));
      DaprBulkAppResponse response = getDaprBulkAppResponse(statuses, b);

      List<DaprBulkAppResponseEntry> statusesFromGet = response.getStatuses();
      assertEquals(2, statusesFromGet.size());
      assertEquals("1", statusesFromGet.get(0).getEntryID());
      assertEquals("2", statusesFromGet.get(1).getEntryID());
    }
  }
}
