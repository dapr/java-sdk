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


import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QueryStateResponse<T> {

  private final List<QueryStateItem<T>> results;

  private final String token;

  private Map<String, String> metadata;

  public QueryStateResponse(List<QueryStateItem<T>> results, String token) {
    this.results = results == null ? null : Collections.unmodifiableList(results);
    this.token = token;
  }

  public List<QueryStateItem<T>> getResults() {
    return results;
  }

  public String getToken() {
    return token;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public QueryStateResponse<T> setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }
}
