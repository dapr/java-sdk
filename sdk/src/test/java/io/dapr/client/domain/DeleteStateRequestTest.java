package io.dapr.client.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;

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
    Assertions.assertNotSame(request.getMetadata(), initial, "Should not be same map");
  }
}
