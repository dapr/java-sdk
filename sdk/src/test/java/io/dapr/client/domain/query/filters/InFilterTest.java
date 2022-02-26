package io.dapr.client.domain.query.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class InFilterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  String json = "{\"IN\":{\"key\":[1.5,44.0]}}";

  @Test
  public void testSerialization() throws JsonProcessingException {
    InFilter<?> filter = new InFilter<>("key", 1.5, 44.0);

    Assert.assertEquals(json, MAPPER.writeValueAsString(filter));
  }

  @Test
  public void testDeserialization() throws JsonProcessingException {
    InFilter<?> filter = MAPPER.readValue(json, InFilter.class);
    Assert.assertEquals("key", filter.getKey());
    Assert.assertArrayEquals(new Double[]{ 1.5, 44.0 }, filter.getValues().toArray());
  }

  @Test
  public void testValidation() {
    InFilter<?> filter = new InFilter<>(null, "val");
    Assert.assertFalse(filter.isValid());


    filter = new InFilter<>("", "");
    Assert.assertFalse(filter.isValid());

    filter = new InFilter<>("", true);
    Assert.assertFalse(filter.isValid());

    filter = new InFilter<>("   ", "valid");
    Assert.assertFalse(filter.isValid());

    filter = new InFilter<>("valid", "");
    Assert.assertTrue(filter.isValid());

    filter = new InFilter<>("valid", "1.5", "2.5");
    Assert.assertTrue(filter.isValid());
  }
}
