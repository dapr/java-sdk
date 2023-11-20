package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EqFilterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  String json = "{\"EQ\":{\"key\":1.5}}";

  @Test
  public void testSerialization() throws JsonProcessingException {
    EqFilter<?> filter = new EqFilter<>("key", 1.5);

    Assertions.assertEquals(json, MAPPER.writeValueAsString(filter));
  }

  @Test
  public void testDeserialization() throws JsonProcessingException {
    EqFilter<?> filter = MAPPER.readValue(json, EqFilter.class);
    Assertions.assertEquals("key", filter.getKey());
    Assertions.assertEquals(1.5, filter.getValue());
  }

  @Test
  public void testValidation() {
    EqFilter<?> filter = new EqFilter<>(null, "val");
    Assertions.assertFalse(filter.isValid());


    filter = new EqFilter<>("", "");
    Assertions.assertFalse(filter.isValid());

    filter = new EqFilter<>("", true);
    Assertions.assertFalse(filter.isValid());

    filter = new EqFilter<>("   ", "valid");
    Assertions.assertFalse(filter.isValid());

    filter = new EqFilter<>("valid", "");
    Assertions.assertTrue(filter.isValid());
  }
}
