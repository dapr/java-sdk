/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/
package io.dapr.client.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

  @Test
  @DisplayName("Should set states as null when the argument is null")
  void testSetStateWithNullParameter() {

    SaveStateRequest request = new SaveStateRequest(STORE_NAME);
    request.setStates((List<State<?>>) null);
    List<State<?>> states = request.getStates();

    assertThat(states).isNull();
  }
}
