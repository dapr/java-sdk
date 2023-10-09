package io.dapr.client.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SaveStateRequestTest {

  private String STORE_NAME = "STORE";

  @Test
  public void testSetStates(){
    SaveStateRequest request = new SaveStateRequest(STORE_NAME);
    // Null check
    assertNull(request.getStates());
    // Modifiability check
    List<State<?>> states = new ArrayList<>();
    states.add(new State<>("test"));
    request.setStates(states);
    List<State<?>> initial = request.getStates();
    request.setStates(states);
    assertNotSame( request.getStates(), initial, "Should not be same list");

    // With var args method
    request.setStates(new State<>("test var args 1"), new State<>("test var args 2"));
    assertEquals("test var args 1", request.getStates().get(0).getKey(), "Value incorrectly set");
    assertEquals("test var args 2", request.getStates().get(1).getKey(), "Value incorrectly set");
  }
}