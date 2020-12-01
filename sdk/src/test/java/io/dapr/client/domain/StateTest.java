package io.dapr.client.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

public class StateTest {

  private static final String KEY = "key";
  private static final String ETAG = "111";
  private static final Map<String, String> METADATA = new HashMap<>();
  static {
    METADATA.put("key1", "value1");
    METADATA.put("key2", "value2");
    METADATA.put("key3", "value3");
    METADATA.put("key4", "value4");
  }
  private static final StateOptions OPTIONS = new StateOptions(StateOptions.Consistency.STRONG,
      StateOptions.Concurrency.FIRST_WRITE);

  @Test
  public void testSimpleStringState() {
    State<String> state = new State<>("value", KEY, ETAG);
    assertNotNull(state);
    assertNull(state.getError());
    assertNull(state.getOptions());
    assertEquals(KEY, state.getKey());
    assertEquals(ETAG, state.getEtag());
    assertEquals("value", state.getValue());
    String expected = "StateKeyValue{"
        + "value=value"
        + ", key='" + KEY + "'"
        + ", etag='" + ETAG + "'"
        + ", metadata={'null'}"
        + ", error='null'"
        + ", options={'null'}"
        + "}";
    assertEquals(expected, state.toString());
  }

  @Test
  public void testStringState() {
    State<String> state = new State<>(KEY, ETAG, OPTIONS);
    assertNotNull(state);
    assertNull(state.getError());
    assertNull(state.getValue());
    assertEquals(OPTIONS, state.getOptions());
    assertEquals(KEY, state.getKey());
    assertEquals(ETAG, state.getEtag());
    String expected = "StateKeyValue{"
        + "value=null"
        + ", key='" + KEY + "'"
        + ", etag='" + ETAG + "'"
        + ", metadata={'null'}"
        + ", error='null'"
        + ", options={'" + OPTIONS.toString() + "'}"
        + "}";
    assertEquals(expected, state.toString());
  }

  @Test
  public void testSimpleStringStateWithOptions() {
    State<String> state = new State<>("value", KEY, ETAG, OPTIONS);
    assertNotNull(state);
    assertNull(state.getError());
    assertEquals(OPTIONS, state.getOptions());
    assertEquals(KEY, state.getKey());
    assertEquals(ETAG, state.getEtag());
    String expected = "StateKeyValue{"
        + "value=value"
        + ", key='" + KEY + "'"
        + ", etag='" + ETAG + "'"
        + ", metadata={'null'}"
        + ", error='null'"
        + ", options={'" + OPTIONS.toString() + "'}"
        + "}";
    assertEquals(expected, state.toString());
    assertEquals("value", state.getValue());
  }

  @Test
  public void testEqualsAndHashcode() {
    State<String> state1 = new State<>("value", KEY, ETAG, new HashMap<>(METADATA), OPTIONS);
    State<String> state2 = new State<>("value", KEY, ETAG, new HashMap<>(METADATA), OPTIONS);
    assertEquals(state1.toString(), state2.toString());
    assertEquals(state1.hashCode(), state2.hashCode());
    assertEquals(state1, state2);

    Map<String, String> metadata3 = new HashMap<>(METADATA);
    metadata3.put("key5", "value5");
    State<String> state3 = new State<>("value", KEY, ETAG, metadata3, OPTIONS);
    assertNotEquals(state1.toString(), state3.toString());
    assertNotEquals(state1.hashCode(), state3.hashCode());
    assertNotEquals(state1, state3);

    Map<String, String> metadata4 = new HashMap<>(METADATA);
    metadata4.remove("key1");
    State<String> state4 = new State<>("value", KEY, ETAG, metadata4, OPTIONS);
    assertNotEquals(state1.toString(), state4.toString());
    assertNotEquals(state1.hashCode(), state4.hashCode());
    assertNotEquals(state1, state4);
  }
}
