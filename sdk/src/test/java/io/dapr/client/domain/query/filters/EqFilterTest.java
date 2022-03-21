package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EqFilterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  String json = "{\"EQ\":{\"key\":1.5}}";

  @Test
  public void testSerialization() throws JsonProcessingException {
    EqFilter<?> filter = new EqFilter<>("key", 1.5);

    Assert.assertEquals(json, MAPPER.writeValueAsString(filter));
  }

  @Test
  public void testDeserialization() throws JsonProcessingException {
    EqFilter<?> filter = MAPPER.readValue(json, EqFilter.class);
    Assert.assertEquals("key", filter.getKey());
    Assert.assertEquals(1.5, filter.getValue());
  }

  @Test
  public void testValidation() {
    EqFilter<?> filter = new EqFilter<>(null, "val");
    Assert.assertFalse(filter.isValid());


    filter = new EqFilter<>("", "");
    Assert.assertFalse(filter.isValid());

    filter = new EqFilter<>("", true);
    Assert.assertFalse(filter.isValid());

    filter = new EqFilter<>("   ", "valid");
    Assert.assertFalse(filter.isValid());

    filter = new EqFilter<>("valid", "");
    Assert.assertTrue(filter.isValid());
  }
}
