/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.spring.data.repository.query;

import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.ObjectUtils;
import org.springframework.util.comparator.Comparators;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class DaprPredicateBuilder {

  private final Part part;

  private DaprPredicateBuilder(Part part) {
    this.part = part;
  }

  static DaprPredicateBuilder propertyValueOf(Part part) {
    return new DaprPredicateBuilder(part);
  }

  Predicate<Object> isTrue() {
    return new DaprPredicate(part.getProperty(), true);
  }

  public Predicate<Object> isFalse() {
    return new DaprPredicate(part.getProperty(), false);
  }

  public Predicate<Object> isEqualTo(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> {
      if (!ObjectUtils.nullSafeEquals(Part.IgnoreCaseType.NEVER, part.shouldIgnoreCase())) {
        if (o instanceof String s1 && value instanceof String s2) {
          return s1.equalsIgnoreCase(s2);
        }
      }

      return ObjectUtils.nullSafeEquals(o, value);
    });
  }

  public Predicate<Object> isNull() {
    return new DaprPredicate(part.getProperty(), null, Objects::isNull);
  }

  public Predicate<Object> isNotNull() {
    return isNull().negate();
  }

  public Predicate<Object> isLessThan(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> Comparators.nullsHigh().compare(o, value) < 0);
  }

  public Predicate<Object> isLessThanEqual(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> Comparators.nullsHigh().compare(o, value) <= 0);
  }

  public Predicate<Object> isGreaterThan(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> Comparators.nullsHigh().compare(o, value) > 0);
  }

  public Predicate<Object> isGreaterThanEqual(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> Comparators.nullsHigh().compare(o, value) >= 0);
  }

  public Predicate<Object> matches(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> {
      if (o == null || value == null) {
        return ObjectUtils.nullSafeEquals(o, value);
      }

      if (value instanceof Pattern pattern) {
        return pattern.matcher(o.toString()).find();
      }

      return o.toString().matches(value.toString());
    });
  }

  public Predicate<Object> in(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> {
      if (value instanceof Collection<?> collection) {
        if (o instanceof Collection<?> subSet) {
          return collection.containsAll(subSet);
        }

        return collection.contains(o);
      }

      if (ObjectUtils.isArray(value)) {
        return ObjectUtils.containsElement(ObjectUtils.toObjectArray(value), value);
      }

      return false;
    });
  }

  public Predicate<Object> contains(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> {
      if (o == null) {
        return false;
      }

      if (o instanceof Collection<?> collection) {
        return collection.contains(value);
      }

      if (ObjectUtils.isArray(o)) {
        return ObjectUtils.containsElement(ObjectUtils.toObjectArray(o), value);
      }

      if (o instanceof Map<?, ?> map) {
        return map.containsValue(value);
      }

      if (value == null) {
        return false;
      }

      String s = o.toString();

      if (ObjectUtils.nullSafeEquals(Part.IgnoreCaseType.NEVER, part.shouldIgnoreCase())) {
        return s.contains(value.toString());
      }

      return s.toLowerCase().contains(value.toString().toLowerCase());
    });
  }

  public Predicate<Object> startsWith(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> {
      if (!(o instanceof String s)) {
        return false;
      }

      if (ObjectUtils.nullSafeEquals(Part.IgnoreCaseType.NEVER, part.shouldIgnoreCase())) {
        return s.startsWith(value.toString());
      }

      return s.toLowerCase().startsWith(value.toString().toLowerCase());
    });
  }

  public Predicate<Object> endsWith(Object value) {
    return new DaprPredicate(part.getProperty(), value, o -> {
      if (!(o instanceof String s)) {
        return false;
      }

      if (ObjectUtils.nullSafeEquals(Part.IgnoreCaseType.NEVER, part.shouldIgnoreCase())) {
        return s.endsWith(value.toString());
      }

      return s.toLowerCase().endsWith(value.toString().toLowerCase());
    });
  }
}
