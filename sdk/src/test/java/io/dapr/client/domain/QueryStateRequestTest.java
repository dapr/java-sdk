package io.dapr.client.domain;

import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.filters.EqFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryStateRequestTest {

  private String STORE_NAME = "STORE";
  private String KEY = "KEY";

  @Test
  public void testSetMetadata() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
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

  @Test
  public void testSetNullQuery() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
    assertThrows(IllegalArgumentException.class, () -> request.setQuery(null));
  }

  @Test
  public void testSetNullFilterQuery() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
    Query query = new Query();
    assertThrows(IllegalArgumentException.class, () -> request.setQuery(query));
  }

  @Test
  public void testSetQuery() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
    Query query = new Query();
    query.setFilter(new EqFilter<>("key", "value"));
    request.setQuery(query);
    Assertions.assertEquals(query, request.getQuery());
  }
}
