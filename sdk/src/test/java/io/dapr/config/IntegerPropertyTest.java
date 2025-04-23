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
package io.dapr.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IntegerPropertyTest {

  @Test
  @DisplayName("Should create IntegerProperty correctly")
  void shouldCreateIntegerPropertyCorrectly() {
    IntegerProperty property = new IntegerProperty("int", "INT", 0);
    Integer twoHundreds = property.parse("200");
    assertThat(twoHundreds).isEqualTo(200);
  }

  @Test
  @DisplayName("Should throws NumberFormatException when parsing non number")
  void shouldThrowsExceptionWhenParsingNonNumber() {
    IntegerProperty property = new IntegerProperty("int", "INT", 0);
    assertThatThrownBy(() -> property.parse("TWO_THOUSANDS"));
  }

}
