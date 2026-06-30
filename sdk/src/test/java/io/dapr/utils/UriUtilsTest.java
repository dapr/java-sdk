/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.utils;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UriUtilsTest {

  private static final URI BASE_URI = URI.create("http://localhost:3500/v1.0/invoke/orderprocessor/method/");

  @Test
  public void encodePath_encodesSegmentsWithSpacesPreservingSeparators() {
    assertEquals("api/some-resource/Name%20With%20Spaces/versions",
        UriUtils.encodePath("api/some-resource/Name With Spaces/versions"));
  }

  @Test
  public void encodePath_encodesIllegalCharsAndAppendsQueryUnchanged() {
    assertEquals("orders/a%20b/c%23d?status=open&page=2",
        UriUtils.encodePath("orders/a b/c#d?status=open&page=2"));
  }

  @Test
  public void encodePath_preservesLeadingSlash() {
    assertEquals("/v1.0/healthz/outbound", UriUtils.encodePath("/v1.0/healthz/outbound"));
  }

  @Test
  public void encodePath_rejectsNull() {
    assertThrows(NullPointerException.class, () -> UriUtils.encodePath(null));
  }

  @Test
  public void resolve_resolvesRelativePathAgainstBaseUri() {
    assertEquals(URI.create("http://localhost:3500/v1.0/invoke/orderprocessor/method/orders/42"),
        UriUtils.resolve(BASE_URI, "orders/42"));
  }

  @Test
  public void resolve_rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> UriUtils.resolve(null, "orders/42"));
    assertThrows(NullPointerException.class, () -> UriUtils.resolve(BASE_URI, null));
  }

  @Test
  public void resolve_rejectsIllegalCharactersWithoutLeakingInput() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> UriUtils.resolve(BASE_URI, "orders/secret-token-abc 123"));

    assertTrue(exception.getMessage().contains("encodePath"));
    // The raw input must not leak into the message or via the wrapped cause.
    assertFalse(exception.getMessage().contains("secret-token-abc"));
    assertNull(exception.getCause());
  }
}
