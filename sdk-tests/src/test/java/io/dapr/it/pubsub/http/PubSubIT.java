/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.it.pubsub.http;

import io.dapr.it.testcontainers.pubsub.http.DaprPubSubIT;

import java.util.Objects;

/**
 * Backward-compatible test class for PubSub ITs.
 *
 * <p>This class now runs the Testcontainers-based pub/sub integration suite
 * by inheriting from {@link DaprPubSubIT}. The nested payload types remain
 * here to avoid broad refactors in related test components.</p>
 */
public class PubSubIT extends DaprPubSubIT {

  public static class MyObject {
    private String id;

    public String getId() {
      return this.id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  public static class ConvertToLong {
    private Long value;

    public ConvertToLong setVal(Long value) {
      this.value = value;
      return this;
    }

    public Long getValue() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ConvertToLong that = (ConvertToLong) o;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }
}
