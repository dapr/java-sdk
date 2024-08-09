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

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * This class is copied from https://github.com/spring-projects/spring-data-keyvalue/blob/ff441439124585042dd0cbff952f977a343444d2/src/main/java/org/springframework/data/keyvalue/repository/query/PredicateQueryCreator.java#L46
 * because it has private accessors to internal classes, making it impossible to extend or use the original
 * This requires to be created from scratch to not use predicates, but this is only worth it if we can prove these
 * abstractions are worth the time.
 */
public class DaprPredicateQueryCreator extends AbstractQueryCreator<KeyValueQuery<Predicate<?>>, Predicate<?>> {

  public DaprPredicateQueryCreator(PartTree tree, ParameterAccessor parameters) {
    super(tree, parameters);
  }

  @Override
  protected Predicate<?> create(Part part, Iterator<Object> iterator) {
    DaprPredicateBuilder daprPredicateBuilder = DaprPredicateBuilder.propertyValueOf(part);

    switch (part.getType()) {
      case TRUE:
        return daprPredicateBuilder.isTrue();
      case FALSE:
        return daprPredicateBuilder.isFalse();
      case SIMPLE_PROPERTY:
        return daprPredicateBuilder.isEqualTo(iterator.next());
      case IS_NULL:
        return daprPredicateBuilder.isNull();
      case IS_NOT_NULL:
        return daprPredicateBuilder.isNotNull();
      case LIKE:
        return daprPredicateBuilder.contains(iterator.next());
      case STARTING_WITH:
        return daprPredicateBuilder.startsWith(iterator.next());
      case AFTER:
      case GREATER_THAN:
        return daprPredicateBuilder.isGreaterThan(iterator.next());
      case GREATER_THAN_EQUAL:
        return daprPredicateBuilder.isGreaterThanEqual(iterator.next());
      case BEFORE:
      case LESS_THAN:
        return daprPredicateBuilder.isLessThan(iterator.next());
      case LESS_THAN_EQUAL:
        return daprPredicateBuilder.isLessThanEqual(iterator.next());
      case ENDING_WITH:
        return daprPredicateBuilder.endsWith(iterator.next());
      case BETWEEN:
        return daprPredicateBuilder.isGreaterThan(iterator.next())
            .and(daprPredicateBuilder.isLessThan(iterator.next()));
      case REGEX:
        return daprPredicateBuilder.matches(iterator.next());
      case IN:
        return daprPredicateBuilder.in(iterator.next());
      default:
        throw new InvalidDataAccessApiUsageException(String.format("Found invalid part '%s' in query", part.getType()));

    }
  }

  @Override
  protected Predicate<?> and(Part part, Predicate<?> base, Iterator<Object> iterator) {
    return base.and((Predicate) create(part, iterator));
  }

  @Override
  protected Predicate<?> or(Predicate<?> base, Predicate<?> criteria) {
    return base.or((Predicate) criteria);
  }

  @Override
  protected KeyValueQuery<Predicate<?>> complete(@Nullable Predicate<?> criteria, Sort sort) {
    if (criteria == null) {
      return new KeyValueQuery<>(it -> true, sort);
    }
    return new KeyValueQuery<>(criteria, sort);
  }

}
