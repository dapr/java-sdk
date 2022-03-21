package io.dapr.client.domain.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.query.filters.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class QueryTest {

  ObjectMapper mapper = new ObjectMapper();
  String json = "{\"filter\":{\"AND\":[{\"EQ\":{\"key\":\"value\"}},{\"IN\":{\"key2\":[\"v1\",\"v2\"]}}," +
      "{\"OR\":[{\"EQ\":{\"v2\":true}},{\"IN\":{\"v3\":[1.3,1.5]}}]}]}," +
      "\"sort\":[{\"key\":\"value.person.org\",\"order\":\"ASC\"},{\"key\":\"value.state\",\"order\":\"DESC\"}]," +
      "\"page\":{\"limit\":10,\"token\":\"test-token\"}}";

  @Test
  public void testQuerySerialize() throws JsonProcessingException {
    Query q = new Query();

    AndFilter filter = new AndFilter();
    filter.addClause(new EqFilter<>("key", "value"));
    filter.addClause(new InFilter<>("key2", "v1", "v2"));

    OrFilter orFilter = new OrFilter();
    orFilter.addClause(new EqFilter<>("v2", true));
    orFilter.addClause(new InFilter<>("v3", 1.3, 1.5));

    filter.addClause(orFilter);

    // Add Filter
    q.setFilter(filter);
    q.setPagination(new Pagination(10, "test-token"));
    q.setSort(Arrays.asList(new Sorting("value.person.org", Sorting.Order.ASC),
        new Sorting("value.state", Sorting.Order.DESC)));
    Assert.assertEquals(json, mapper.writeValueAsString(q));
  }


  @Test
  public void testQueryDeserialize() throws JsonProcessingException {


    Query query = mapper.readValue(json, Query.class);
    Assert.assertNotNull(query.getPagination());
    Assert.assertNotNull(query.getFilter());
    Assert.assertNotNull(query.getSort());

    // Assert Pagination
    Assert.assertEquals(10, query.getPagination().getLimit());
    Assert.assertEquals("test-token", query.getPagination().getToken());

    // Assert Sort
    Assert.assertEquals(2, query.getSort().size());
    Assert.assertEquals("value.person.org", query.getSort().get(0).getKey());
    Assert.assertEquals(Sorting.Order.ASC, query.getSort().get(0).getOrder());
    Assert.assertEquals("value.state", query.getSort().get(1).getKey());
    Assert.assertEquals(Sorting.Order.DESC, query.getSort().get(1).getOrder());

    // Assert Filter
    // Top level AND filter
    Assert.assertEquals("AND", query.getFilter().getName());
    // Type cast to AND filter
    AndFilter filter = (AndFilter) query.getFilter();
    // Assert 3 AND clauses
    Assert.assertEquals(3, filter.getClauses().length);
    Filter<?>[] andClauses = filter.getClauses();
    // First EQ
    Assert.assertEquals("EQ", andClauses[0].getName());
    Assert.assertSame(EqFilter.class, andClauses[0].getClass());
    EqFilter<?> eq = (EqFilter<?>) andClauses[0];
    Assert.assertEquals("key", eq.getKey());
    Assert.assertEquals("value", eq.getValue());
    // Second IN
    Assert.assertEquals("IN", andClauses[1].getName());
    Assert.assertSame(InFilter.class, andClauses[1].getClass());
    InFilter<?> in = (InFilter<?>) andClauses[1];
    Assert.assertEquals("key2", in.getKey());
    Assert.assertArrayEquals(new String[]{ "v1", "v2" }, in.getValues().toArray());
    // Third OR
    Assert.assertEquals("OR", andClauses[2].getName());
    Assert.assertSame(OrFilter.class, andClauses[2].getClass());
    OrFilter orFilter = (OrFilter) andClauses[2];
    Filter<?>[] orClauses = orFilter.getClauses();
    // First EQ in OR
    Assert.assertEquals("EQ", orClauses[0].getName());
    Assert.assertSame(EqFilter.class, orClauses[0].getClass());
    eq = (EqFilter<?>) orClauses[0];
    Assert.assertEquals("v2", eq.getKey());
    Assert.assertEquals(true, eq.getValue());
    // Second IN in OR
    Assert.assertEquals("IN", orClauses[1].getName());
    Assert.assertSame(InFilter.class, orClauses[1].getClass());
    in = (InFilter<?>) orClauses[1];
    Assert.assertEquals("v3", in.getKey());
    Assert.assertArrayEquals(new Double[]{ 1.3, 1.5 }, in.getValues().toArray());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQueryInValidFilter() throws JsonProcessingException {
    Query q = new Query();

    AndFilter filter = new AndFilter();
    filter.addClause(new EqFilter<>("key", "value"));
    filter.addClause(new InFilter<>("key2", "v1", "v2"));

    OrFilter orFilter = new OrFilter();
    orFilter.addClause(new EqFilter<>("v2", true));
    // invalid OR filter

    filter.addClause(orFilter);

    // Add Filter
    q.setFilter(filter);
  }
}