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

package io.dapr.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dapr.client.domain.query.Query;

import java.util.Collections;
import java.util.Map;

public class QueryStateRequest {

  @JsonIgnore
  private final String storeName;

  private Query query;

  private String queryString;

  @JsonIgnore
  private Map<String, String> metadata;

  public QueryStateRequest(String storeName) {
    this.storeName = storeName;
  }

  public String getStoreName() {
    return storeName;
  }

  public Query getQuery() {
    return query;
  }

  /**
   * Validate and set the query field. Mutually exclusive with the queryString field in this instance.
   *
   * @param query Valid Query domain object.
   * @return This instance.
   */
  public QueryStateRequest setQuery(Query query) {
    if (this.queryString != null) {
      throw new IllegalArgumentException("queryString filed is already set in the request. query field cannot be set.");
    }
    if (query == null || query.getFilter() == null) {
      throw new IllegalArgumentException("query cannot be null or with null filter");
    }
    this.query = query;
    return this;
  }

  public String getQueryString() {
    return queryString;
  }

  /**
   * Validate and set the queryString field. Mutually exclusive with the query field in this instance.
   *
   * @param queryString String value of the query.
   * @return This request object for fluent API.
   */
  public QueryStateRequest setQueryString(String queryString) {
    if (this.query != null) {
      throw new IllegalArgumentException("query filed is already set in the request. queryString field cannot be set.");
    }
    if (queryString == null || queryString.isEmpty()) {
      throw new IllegalArgumentException("queryString cannot be null or blank");
    }
    this.queryString = queryString;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public QueryStateRequest setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }
}
