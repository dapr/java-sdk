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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.utils.TypeRef;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link org.springframework.data.keyvalue.core.KeyValueAdapter} implementation for MySQL.
 */
@SuppressWarnings("AbbreviationAsWordInName")
public class MySQLDaprKeyValueAdapter extends AbstractDaprKeyValueAdapter {
  private static final String DELETE_BY_KEYSPACE_PATTERN = "delete from state where id LIKE '%s'";
  private static final String SELECT_BY_KEYSPACE_PATTERN = "select value from state where id LIKE '%s'";
  private static final String SELECT_BY_FILTER_PATTERN =
      "select value from state where id LIKE '%s' and JSON_EXTRACT(value, %s) = %s";
  private static final String COUNT_BY_KEYSPACE_PATTERN = "select count(*) as value from state where id LIKE '%s'";
  private static final String COUNT_BY_FILTER_PATTERN =
      "select count(*) as value from state where id LIKE '%s' and JSON_EXTRACT(value, %s) = %s";

  private static final TypeRef<List<JsonNode>> FILTER_TYPE_REF = new TypeRef<>() {
  };
  private static final TypeRef<List<JsonNode>> COUNT_TYPE_REF = new TypeRef<>() {
  };
  private static final SpelExpressionParser PARSER = new SpelExpressionParser();
  private static final JsonPointer VALUE_POINTER = JsonPointer.compile("/value");

  private final DaprClient daprClient;
  private final ObjectMapper mapper;
  private final String stateStoreName;
  private final String bindingName;

  /**
   * Constructs a {@link MySQLDaprKeyValueAdapter}.
   *
   * @param daprClient     The Dapr client.
   * @param mapper         The object mapper.
   * @param stateStoreName The state store name.
   * @param bindingName    The binding name.
   */
  public MySQLDaprKeyValueAdapter(DaprClient daprClient, ObjectMapper mapper, String stateStoreName,
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
    List<JsonNode> result = queryUsingBinding(sql, FILTER_TYPE_REF);

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
    List<JsonNode> result = queryUsingBinding(sql, FILTER_TYPE_REF);

    return convertValues(result, type);
  }

  @Override
  public long count(String keyspace) {
    Assert.hasText(keyspace, "Keyspace must not be empty");

    String sql = createSql(COUNT_BY_KEYSPACE_PATTERN, keyspace);
    List<JsonNode> result = queryUsingBinding(sql, COUNT_TYPE_REF);

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
    List<JsonNode> result = queryUsingBinding(sql, COUNT_TYPE_REF);

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
    SpelExpression expression = PARSER.parseRaw(criteria.toString());
    SpelNode leftNode = expression.getAST().getChild(0);
    SpelNode rightNode = expression.getAST().getChild(1);
    String left = String.format("'$.%s'", leftNode.toStringAST());
    String right = rightNode.toStringAST();

    return String.format(sqlPattern, keyspaceFilter, left, right);
  }

  private void execUsingBinding(String sql) {
    Map<String, String> meta = Collections.singletonMap("sql", sql);

    daprClient.invokeBinding(bindingName, "exec", null, meta).block();
  }

  private <T> T queryUsingBinding(String sql, TypeRef<T> typeRef) {
    Map<String, String> meta = Collections.singletonMap("sql", sql);

    return daprClient.invokeBinding(bindingName, "query", null, meta, typeRef).block();
  }

  private <T> List<T> convertValues(List<JsonNode> values, Class<T> type) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }

    return values.stream()
        .map(value -> convertValue(value, type))
        .collect(Collectors.toList());
  }

  private <T> T convertValue(JsonNode value, Class<T> type) {
    JsonNode valueNode = value.at(VALUE_POINTER);

    if (valueNode.isMissingNode()) {
      throw new IllegalStateException("Value is missing");
    }

    try {
      // The value is stored as a base64 encoded string and wrapped in quotes
      // hence we need to remove the quotes and then decode
      String rawValue = valueNode.toString().replace("\"", "");
      byte[] decodedValue = Base64.getDecoder().decode(rawValue);

      return mapper.readValue(decodedValue, type);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private long extractCount(List<JsonNode> values) {
    if (values == null || values.isEmpty()) {
      return 0;
    }

    JsonNode valueNode = values.get(0).at(VALUE_POINTER);

    if (valueNode.isMissingNode()) {
      throw new IllegalStateException("Count value is missing");
    }

    if (!valueNode.isNumber()) {
      throw new IllegalStateException("Count value is not a number");
    }

    return valueNode.asLong();
  }
}
