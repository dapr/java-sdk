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

import io.dapr.client.DaprHttp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class HttpExtensionTest {


  @Test
  @DisplayName("Should encode query params correctly")
  void shouldEncodeQueryParamsCorrectly() {
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET,
        Map.of("traceparent", List.of("00-4bf92f3577b34da6a3ce929d0e0e4733-00f067aa0ba902b7-01")), Map.of());

    String encoded = httpExtension.encodeQueryString();

    Assertions.assertEquals("traceparent=00-4bf92f3577b34da6a3ce929d0e0e4733-00f067aa0ba902b7-01", encoded);
  }

  @Test
  @DisplayName("Should encode multiple values for same query param key")
  void shouldEncodeMultipleValuesForSameQueryParamKey() {
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET,
        Map.of("category", List.of("books", "electronics")), Map.of());

    String encoded = httpExtension.encodeQueryString();

    Assertions.assertEquals("category=books&category=electronics", encoded);
  }

  @Test
  @DisplayName("Should encode query param with spaces, accents, and special characters")
  void shouldEncodeQueryParamWithSpacesAndSpecialCharacters() {
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET,
        Map.of("user name", List.of("John Doë & Co.")), Map.of());

    String encoded = httpExtension.encodeQueryString();

    Assertions.assertEquals("user+name=John+Do%C3%AB+%26+Co.", encoded);
  }

  @Test
  @DisplayName("PATCH constant should use PATCH HTTP method")
  void patchConstantShouldUsePatchHttpMethod() {
    Assertions.assertEquals(DaprHttp.HttpMethods.PATCH, HttpExtension.PATCH.getMethod());
    Assertions.assertTrue(HttpExtension.PATCH.getQueryParams().isEmpty());
    Assertions.assertTrue(HttpExtension.PATCH.getHeaders().isEmpty());
  }

}
