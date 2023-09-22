package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OrFilterTest {
  private final static ObjectMapper MAPPER = new ObjectMapper();

  private String json = "{\"OR\":[{\"EQ\":{\"key\":\"value\"}},{\"IN\":{\"key2\":[\"v1\",\"v2\"]}}]}";

  @SuppressWarnings("rawtypes")
  @Test
  public void testSerialization() throws JsonProcessingException {
    OrFilter filter = new OrFilter();
    filter.addClause(new EqFilter<>("key", "value"));
    filter.addClause(new InFilter<>("key2", "v1", "v2"));

    Assertions.assertEquals(json, MAPPER.writeValueAsString((Filter) filter));
  }

  @Test
  public void testDeserialization() throws JsonProcessingException {
    Filter<?> res = MAPPER.readValue(json, Filter.class);

    // Check for AndFilter
    Assertions.assertEquals("OR", res.getName());
    Assertions.assertSame(OrFilter.class, res.getClass());

    OrFilter filter = (OrFilter) res;
    // Check 2 clauses
    Assertions.assertEquals(2, filter.getClauses().length);
    // First EQ
    Assertions.assertSame(EqFilter.class, filter.getClauses()[0].getClass());
    EqFilter<?> eq = (EqFilter<?>) filter.getClauses()[0];
    Assertions.assertEquals("key", eq.getKey());
    Assertions.assertEquals("value", eq.getValue());
    // Second IN
    Assertions.assertSame(InFilter.class, filter.getClauses()[1].getClass());
    InFilter<?> in = (InFilter<?>) filter.getClauses()[1];
    Assertions.assertEquals("key2", in.getKey());
    Assertions.assertArrayEquals(new String[]{ "v1", "v2" }, in.getValues().toArray());
  }

  @Test
  public void testValidation() {
    OrFilter filter = new OrFilter();
    Assertions.assertFalse(filter.isValid());

    filter.addClause(new EqFilter<>("key1", "v2"));
    Assertions.assertFalse(filter.isValid());

    filter.addClause(new EqFilter<>("key2", "v3"));
    Assertions.assertTrue(filter.isValid());
  }
}
