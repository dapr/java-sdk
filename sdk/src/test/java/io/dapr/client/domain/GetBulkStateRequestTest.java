package io.dapr.client.domain;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;


public class GetBulkStateRequestTest {

  private String STORE_NAME = "STORE";

  @Test
  public void testRequestCreation(){
    //List constructor
    GetBulkStateRequest request = new GetBulkStateRequest(STORE_NAME, Collections.singletonList("test key"));
    // check correct list init
    assertEquals(1, request.getKeys().size());
    assertEquals("test key", request.getKeys().get(0));
    // check default parallelism
    assertEquals(1, request.getParallelism());
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    Map<String, String> initial = request.getMetadata();
    request.setMetadata(metadata);
    assertNotSame(request.getMetadata(), initial, "Should not be same map");

    // Var args constructor
    request = new GetBulkStateRequest(STORE_NAME, "test var key");
    // check correct list init
    assertEquals(1, request.getKeys().size());
    assertEquals("test var key", request.getKeys().get(0));
    // check default parallelism
    assertEquals(1, request.getParallelism());
    // check parallelism set correctly
    request.setParallelism(2);
    assertEquals(2, request.getParallelism());
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    initial = request.getMetadata();
    request.setMetadata(metadata);
    assertNotSame( request.getMetadata(), initial, "Should not be same map");
  }
}
