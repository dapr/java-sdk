package io.dapr.client.domain;

import io.dapr.client.domain.query.Query;
import io.dapr.client.domain.query.filters.EqFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

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
    Assert.assertNotSame("Should not be same map", request.getMetadata(), initial);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetNullQuery() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
    request.setQuery(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetNullFilterQuery() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
    Query query = new Query();
    request.setQuery(query);
  }

  @Test
  public void testSetQuery() {
    QueryStateRequest request = new QueryStateRequest(STORE_NAME);
    Query query = new Query();
    query.setFilter(new EqFilter<>("key", "value"));
    request.setQuery(query);
    Assert.assertEquals(query, request.getQuery());
  }
}
