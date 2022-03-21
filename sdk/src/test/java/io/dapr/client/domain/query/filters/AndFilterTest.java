package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class AndFilterTest {

  private final static ObjectMapper MAPPER = new ObjectMapper();

  private String json = "{\"AND\":[{\"EQ\":{\"key\":\"value\"}},{\"IN\":{\"key2\":[\"v1\",\"v2\"]}}]}";

  @SuppressWarnings("rawtypes")
  @Test
  public void testSerialization() throws JsonProcessingException {
    AndFilter filter = new AndFilter();
    filter.addClause(new EqFilter<>("key", "value"));
    filter.addClause(new InFilter<>("key2", "v1", "v2"));

    Assert.assertEquals(json, MAPPER.writeValueAsString((Filter) filter));
  }

  @Test
  public void testDeserialization() throws JsonProcessingException {
    Filter<?> res = MAPPER.readValue(json, Filter.class);

    // Check for AndFilter
    Assert.assertEquals("AND", res.getName());
    Assert.assertSame(AndFilter.class, res.getClass());

    AndFilter filter = (AndFilter) res;
    // Check 2 clauses
    Assert.assertEquals(2, filter.getClauses().length);
    // First EQ
    Assert.assertSame(EqFilter.class, filter.getClauses()[0].getClass());
    EqFilter<?> eq = (EqFilter<?>) filter.getClauses()[0];
    Assert.assertEquals("key", eq.getKey());
    Assert.assertEquals("value", eq.getValue());
    // Second IN
    Assert.assertSame(InFilter.class, filter.getClauses()[1].getClass());
    InFilter<?> in = (InFilter<?>) filter.getClauses()[1];
    Assert.assertEquals("key2", in.getKey());
    Assert.assertArrayEquals(new String[]{ "v1", "v2" }, in.getValues().toArray());
  }

  @Test
  public void testValidation() {
    AndFilter filter = new AndFilter();
    Assert.assertFalse(filter.isValid());

    filter.addClause(new EqFilter<>("key1", "v2"));
    Assert.assertFalse(filter.isValid());

    filter.addClause(new EqFilter<>("key2", "v3"));
    Assert.assertTrue(filter.isValid());
  }

}
