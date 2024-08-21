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

package io.dapr.spring.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.spring.data.repository.query.DaprPredicate;
import io.dapr.utils.TypeRef;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link org.springframework.data.keyvalue.core.KeyValueAdapter} implementation for PostgreSQL.
 */
@SuppressWarnings("AbbreviationAsWordInName")
public class PostgreSQLDaprKeyValueAdapter extends AbstractDaprKeyValueAdapter {
  private static final String DELETE_BY_KEYSPACE_PATTERN = "delete from state where key LIKE '%s'";
  private static final String SELECT_BY_KEYSPACE_PATTERN = "select value from state where key LIKE '%s'";
  private static final String SELECT_BY_FILTER_PATTERN =
      "select value from state where key LIKE '%s' and JSONB_EXTRACT_PATH_TEXT(value, %s) = %s";
  private static final String COUNT_BY_KEYSPACE_PATTERN = "select count(*) as value from state where key LIKE '%s'";
  private static final String COUNT_BY_FILTER_PATTERN =
      "select count(*) as value from state where key LIKE '%s' and JSONB_EXTRACT_PATH_TEXT(value, %s) = %s";

  private static final TypeRef<List<List<Object>>> FILTER_TYPE_REF = new TypeRef<>() {
  };
  private static final TypeRef<List<List<Long>>> COUNT_TYPE_REF = new TypeRef<>() {
  };
  private static final SpelExpressionParser PARSER = new SpelExpressionParser();

  private final DaprClient daprClient;
  private final ObjectMapper mapper;
  private final String stateStoreName;
  private final String bindingName;

  /**
   * Constructs a {@link PostgreSQLDaprKeyValueAdapter}.
   *
   * @param daprClient     The Dapr client.
   * @param mapper         The object mapper.
   * @param stateStoreName The state store name.
   * @param bindingName    The binding name.
   */
  public PostgreSQLDaprKeyValueAdapter(DaprClient daprClient, ObjectMapper mapper, String stateStoreName,
                                       String bindingName) {
    super(daprClient, stateStoreName);

    Assert.notNull(mapper, "ObjectMapper must not be null");
    Assert.hasText(bindingName, "State store binding must not be empty");

    this.daprClient = daprClient;
    this.mapper = mapper;
    this.stateStoreName = stateStoreName;
    this.bindingName = bindingName;
  }

  @Override
  public <T> Iterable<T> getAllOf(String keyspace, Class<T> type) {
    Assert.hasText(keyspace, "Keyspace must not be empty");
    Assert.notNull(type, "Type must not be null");

    String sql = createSql(SELECT_BY_KEYSPACE_PATTERN, keyspace);
    List<List<Object>> result = queryUsingBinding(sql, FILTER_TYPE_REF);

    return convertValues(result, type);
  }

  @Override
  public void deleteAllOf(String keyspace) {
    Assert.hasText(keyspace, "Keyspace must not be empty");

    String sql = createSql(DELETE_BY_KEYSPACE_PATTERN, keyspace);

    execUsingBinding(sql);
  }

  @Override
  public <T> Iterable<T> find(KeyValueQuery<?> query, String keyspace, Class<T> type) {
    Assert.notNull(query, "Query must not be null");
    Assert.hasText(keyspace, "Keyspace must not be empty");
    Assert.notNull(type, "Type must not be null");

    Object criteria = query.getCriteria();

    if (criteria == null) {
      return getAllOf(keyspace, type);
    }

    String sql = createSql(SELECT_BY_FILTER_PATTERN, keyspace, criteria);
    List<List<Object>> result = queryUsingBinding(sql, FILTER_TYPE_REF);

    return convertValues(result, type);
  }

  @Override
  public long count(String keyspace) {
    Assert.hasText(keyspace, "Keyspace must not be empty");

    String sql = createSql(COUNT_BY_KEYSPACE_PATTERN, keyspace);
    List<List<Long>> result = queryUsingBinding(sql, COUNT_TYPE_REF);

    return extractCount(result);
  }

  @Override
  public long count(KeyValueQuery<?> query, String keyspace) {
    Assert.notNull(query, "Query must not be null");
    Assert.hasText(keyspace, "Keyspace must not be empty");

    Object criteria = query.getCriteria();

    if (criteria == null) {
      return count(keyspace);
    }

    String sql = createSql(COUNT_BY_FILTER_PATTERN, keyspace, criteria);
    List<List<Long>> result = queryUsingBinding(sql, COUNT_TYPE_REF);

    return extractCount(result);
  }

  private String getKeyspaceFilter(String keyspace) {
    return String.format("%s||%s-%%", stateStoreName, keyspace);
  }

  private String createSql(String sqlPattern, String keyspace) {
    String keyspaceFilter = getKeyspaceFilter(keyspace);

    return String.format(sqlPattern, keyspaceFilter);
  }

  private String createSql(String sqlPattern, String keyspace, Object criteria) {
    String keyspaceFilter = getKeyspaceFilter(keyspace);

    if (criteria instanceof DaprPredicate daprPredicate) {
      String path = daprPredicate.getPath().toString();
      String pathWithOutType = String.format("'%s'", path.substring(path.indexOf(".") + 1));
      String value = String.format("'%s'", daprPredicate.getValue().toString());

      return String.format(sqlPattern, keyspaceFilter, pathWithOutType, value);
    } else if (criteria instanceof String) {
      SpelExpression expression = PARSER.parseRaw(criteria.toString());
      SpelNode leftNode = expression.getAST().getChild(0);
      SpelNode rightNode = expression.getAST().getChild(1);
      String left = String.format("'%s'", leftNode.toStringAST());
      String right = rightNode.toStringAST();

      return String.format(sqlPattern, keyspaceFilter, left, right);
    }

    return null;
  }

  private void execUsingBinding(String sql) {
    Map<String, String> meta = Collections.singletonMap("sql", sql);

    daprClient.invokeBinding(bindingName, "exec", null, meta).block();
  }

  private <T> T queryUsingBinding(String sql, TypeRef<T> typeRef) {
    Map<String, String> meta = Collections.singletonMap("sql", sql);

    return daprClient.invokeBinding(bindingName, "query", null, meta, typeRef).block();
  }

  private <T> Iterable<T> convertValues(List<List<Object>> values, Class<T> type) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }

    return values.stream()
        .flatMap(Collection::stream)
        .map(value -> convertValue(value, type))
        .collect(Collectors.toList());
  }

  private <T> T convertValue(Object value, Class<T> type) {
    try {
      return mapper.convertValue(value, type);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private long extractCount(List<List<Long>> values) {
    if (values == null || values.isEmpty()) {
      return 0;
    }

    return values.stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList())
        .get(0);
  }
}
