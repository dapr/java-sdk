package io.dapr.client.domain.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.query.filters.AndFilter;
import io.dapr.client.domain.query.filters.EqFilter;
import io.dapr.client.domain.query.filters.Filter;
import io.dapr.client.domain.query.filters.InFilter;
import io.dapr.client.domain.query.filters.OrFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

    filter.addClause((Filter<?>) orFilter);

    // Add Filter
    q.setFilter(filter);
    q.setPagination(new Pagination(10, "test-token"));
    q.setSort(Arrays.asList(new Sorting("value.person.org", Sorting.Order.ASC),
        new Sorting("value.state", Sorting.Order.DESC)));
    Assertions.assertEquals(json, mapper.writeValueAsString(q));
  }


  @Test
  public void testQueryDeserialize() throws JsonProcessingException {


    Query query = mapper.readValue(json, Query.class);
    Assertions.assertNotNull(query.getPagination());
    Assertions.assertNotNull(query.getFilter());
    Assertions.assertNotNull(query.getSort());

    // Assert Pagination
    Assertions.assertEquals(10, query.getPagination().getLimit());
    Assertions.assertEquals("test-token", query.getPagination().getToken());

    // Assert Sort
    Assertions.assertEquals(2, query.getSort().size());
    Assertions.assertEquals("value.person.org", query.getSort().get(0).getKey());
    Assertions.assertEquals(Sorting.Order.ASC, query.getSort().get(0).getOrder());
    Assertions.assertEquals("value.state", query.getSort().get(1).getKey());
    Assertions.assertEquals(Sorting.Order.DESC, query.getSort().get(1).getOrder());

    // Assert Filter
    // Top level AND filter
    Assertions.assertEquals("AND", query.getFilter().getName());
    // Type cast to AND filter
    AndFilter filter = (AndFilter) query.getFilter();
    // Assert 3 AND clauses
    Assertions.assertEquals(3, filter.getClauses().length);
    Filter<?>[] andClauses = filter.getClauses();
    // First EQ
    Assertions.assertEquals("EQ", andClauses[0].getName());
    Assertions.assertSame(EqFilter.class, andClauses[0].getClass());
    EqFilter<?> eq = (EqFilter<?>) andClauses[0];
    Assertions.assertEquals("key", eq.getKey());
    Assertions.assertEquals("value", eq.getValue());
    // Second IN
    Assertions.assertEquals("IN", andClauses[1].getName());
    Assertions.assertSame(InFilter.class, andClauses[1].getClass());
    InFilter<?> in = (InFilter<?>) andClauses[1];
    Assertions.assertEquals("key2", in.getKey());
    Assertions.assertArrayEquals(new String[]{ "v1", "v2" }, in.getValues().toArray());
    // Third OR
    Assertions.assertEquals("OR", andClauses[2].getName());
    Assertions.assertSame(OrFilter.class, andClauses[2].getClass());
    OrFilter orFilter = (OrFilter) andClauses[2];
    Filter<?>[] orClauses = orFilter.getClauses();
    // First EQ in OR
    Assertions.assertEquals("EQ", orClauses[0].getName());
    Assertions.assertSame(EqFilter.class, orClauses[0].getClass());
    eq = (EqFilter<?>) orClauses[0];
    Assertions.assertEquals("v2", eq.getKey());
    Assertions.assertEquals(true, eq.getValue());
    // Second IN in OR
    Assertions.assertEquals("IN", orClauses[1].getName());
    Assertions.assertSame(InFilter.class, orClauses[1].getClass());
    in = (InFilter<?>) orClauses[1];
    Assertions.assertEquals("v3", in.getKey());
    Assertions.assertArrayEquals(new Double[]{ 1.3, 1.5 }, in.getValues().toArray());
  }

  @Test
  public void testQueryInValidFilter() throws JsonProcessingException {
    Query q = new Query();

    AndFilter filter = new AndFilter();
    filter.addClause(new EqFilter<>("key", "value"));
    filter.addClause(new InFilter<>("key2", "v1", "v2"));

    OrFilter orFilter = new OrFilter();
    orFilter.addClause(new EqFilter<>("v2", true));
    // invalid OR filter

    filter.addClause((Filter<?>) orFilter);

    // Add Filter
    assertThrows(IllegalArgumentException.class, () -> q.setFilter(filter));
  }
}