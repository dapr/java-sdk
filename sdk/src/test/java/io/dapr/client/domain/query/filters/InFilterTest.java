package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InFilterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  String json = "{\"IN\":{\"key\":[1.5,44.0]}}";

  @Test
  public void testSerialization() throws JsonProcessingException {
    InFilter<?> filter = new InFilter<>("key", 1.5, 44.0);

    Assertions.assertEquals(json, MAPPER.writeValueAsString(filter));
  }

  @Test
  public void testDeserialization() throws JsonProcessingException {
    InFilter<?> filter = MAPPER.readValue(json, InFilter.class);
    Assertions.assertEquals("key", filter.getKey());
    Assertions.assertArrayEquals(new Double[]{ 1.5, 44.0 }, filter.getValues().toArray());
  }

  @Test
  public void testValidation() {
    InFilter<?> filter = new InFilter<>(null, "val");
    Assertions.assertFalse(filter.isValid());


    filter = new InFilter<>("", "");
    Assertions.assertFalse(filter.isValid());

    filter = new InFilter<>("", true);
    Assertions.assertFalse(filter.isValid());

    filter = new InFilter<>("   ", "valid");
    Assertions.assertFalse(filter.isValid());

    filter = new InFilter<>("valid", "");
    Assertions.assertTrue(filter.isValid());

    filter = new InFilter<>("valid", "1.5", "2.5");
    Assertions.assertTrue(filter.isValid());
  }
}
