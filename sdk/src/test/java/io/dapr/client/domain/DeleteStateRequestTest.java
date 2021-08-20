package io.dapr.client.domain;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

public class DeleteStateRequestTest {

  private String STORE_NAME = "STORE";
  private String KEY = "KEY";

  @Test
  public void testSetMetadata(){
    DeleteStateRequest request = new DeleteStateRequest(STORE_NAME, KEY);
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
}
