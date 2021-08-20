package io.dapr.client.domain;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

public class GetBulkSecretRequestTest {

  private String STORE_NAME = "STORE";

  @Test
  public void testSetMetadata(){
    GetBulkSecretRequest request = new GetBulkSecretRequest(STORE_NAME);
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    Map<String, String> initial = request.getMetadata();
    request.setMetadata(metadata);
    assertNotSame("Should not be same map", request.getMetadata(), initial);
  }
}