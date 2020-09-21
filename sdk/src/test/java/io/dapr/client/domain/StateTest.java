package io.dapr.client.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class StateTest {

  private static final String KEY = "key";
  private static final String ETAG = "111";
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
        + ", error='null'"
        + ", options={'null'}"
        + "}";;
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
        + ", error='null'"
        + ", options={'" + OPTIONS.toString() + "'}"
        + "}";;
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
        + ", error='null'"
        + ", options={'" + OPTIONS.toString() + "'}"
        + "}";;
    assertEquals(expected, state.toString());
    assertEquals("value", state.getValue());
  }
}
