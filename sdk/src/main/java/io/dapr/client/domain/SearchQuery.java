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

package io.dapr.client.domain;

import java.util.List;
import java.util.Map;

/**
 * Selects and shapes the hits of a search: every document, a full-text query or a query in the store's own format,
 * narrowed by an optional filter.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SearchQuery {

  private final boolean matchAll;
  private final String text;
  private final Map<String, Object> nativeQuery;
  private Map<String, Object> filter;
  private Integer topK;
  private String continuationToken;
  private List<String> returnFields;
  private Boolean includeContent;
  private List<String> searchFields;
  private List<SortClause> sort;
  private List<String> highlightFields;

  private SearchQuery(boolean matchAll, String text, Map<String, Object> nativeQuery) {
    this.matchAll = matchAll;
    this.text = text;
    this.nativeQuery = nativeQuery;
  }

  /**
   * Creates a query that matches every document, subject to a filter if one is set.
   *
   * @return the query
   */
  public static SearchQuery matchAll() {
    return new SearchQuery(true, null, null);
  }

  /**
   * Creates a full-text query.
   *
   * @param text the text to search for. Must not be blank.
   * @return the query
   */
  public static SearchQuery byText(String text) {
    return new SearchQuery(false, text, null);
  }

  /**
   * Creates a query in the store's own format, passed to the store as is. Not every store supports this.
   *
   * <p>Values can be null, a Boolean, a Number, a String, a Collection or a Map with string keys. Other types
   * are rejected with an {@link IllegalArgumentException} when the query is sent, and so are numbers that a double
   * cannot hold exactly, such as the long 9007199254740993.
   *
   * @param query the native query. Must not be empty.
   * @return the query
   */
  public static SearchQuery byNative(Map<String, Object> query) {
    return new SearchQuery(false, null, query);
  }

  /**
   * Gets whether this query was created to match every document.
   *
   * @return true for {@link #matchAll()}
   */
  public boolean isMatchAll() {
    return matchAll;
  }

  /**
   * Gets the text to search for.
   *
   * @return the text, or null when this is not a full-text query
   */
  public String getText() {
    return text;
  }

  /**
   * Gets the query in the store's own format.
   *
   * @return the native query, or null when this is not a native query
   */
  public Map<String, Object> getNative() {
    return nativeQuery;
  }

  /**
   * Gets the filter on document content.
   *
   * @return the filter, or null when the query has none
   */
  public Map<String, Object> getFilter() {
    return filter;
  }

  /**
   * Sets a filter on document content, written in the Dapr filter format.
   *
   * <p>Values can be null, a Boolean, a Number, a String, a Collection or a Map with string keys. Other types
   * are rejected with an {@link IllegalArgumentException} when the query is sent, and so are numbers that a double
   * cannot hold exactly, such as the long 9007199254740993.
   *
   * @param filter the filter
   * @return this query for method chaining
   */
  public SearchQuery setFilter(Map<String, Object> filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Gets the maximum number of hits in a page.
   *
   * @return the limit, or null for the component's default
   */
  public Integer getTopK() {
    return topK;
  }

  /**
   * Sets the maximum number of hits in a page. 0 or null uses the component's default.
   *
   * @param topK the limit. Must not be negative.
   * @return this query for method chaining
   */
  public SearchQuery setTopK(Integer topK) {
    this.topK = topK;
    return this;
  }

  /**
   * Gets the token of the page to read.
   *
   * @return the token, or null for the first page
   */
  public String getContinuationToken() {
    return continuationToken;
  }

  /**
   * Sets the token of the page to read, from {@link SearchResponseAlpha1#getContinuationToken()}. A token is bound
   * to the query that produced it, so send the same query with only the token changed.
   *
   * @param continuationToken the token
   * @return this query for method chaining
   */
  public SearchQuery setContinuationToken(String continuationToken) {
    this.continuationToken = continuationToken;
    return this;
  }

  /**
   * Gets the content fields to return with each hit.
   *
   * @return the fields, or null when the query does not restrict them
   */
  public List<String> getReturnFields() {
    return returnFields;
  }

  /**
   * Restricts the content returned with each hit to these fields, as top-level or dotted nested keys. Set
   * {@link #setIncludeContent(Boolean)} as well, because stores differ on whether return fields alone return content.
   *
   * @param returnFields the fields
   * @return this query for method chaining
   */
  public SearchQuery setReturnFields(List<String> returnFields) {
    this.returnFields = returnFields;
    return this;
  }

  /**
   * Gets whether hits include the document content.
   *
   * @return true to include the content, or null for the default (false)
   */
  public Boolean getIncludeContent() {
    return includeContent;
  }

  /**
   * Sets whether hits include the document content. The default is false.
   *
   * @param includeContent true to include the content
   * @return this query for method chaining
   */
  public SearchQuery setIncludeContent(Boolean includeContent) {
    this.includeContent = includeContent;
    return this;
  }

  /**
   * Gets the content fields that a full-text query searches.
   *
   * @return the fields, or null when the query does not restrict them
   */
  public List<String> getSearchFields() {
    return searchFields;
  }

  /**
   * Restricts a full-text query to these content fields, as top-level or dotted nested keys.
   *
   * @param searchFields the fields
   * @return this query for method chaining
   */
  public SearchQuery setSearchFields(List<String> searchFields) {
    this.searchFields = searchFields;
    return this;
  }

  /**
   * Gets the sort keys.
   *
   * @return the sort keys, or null to order hits by relevance
   */
  public List<SortClause> getSort() {
    return sort;
  }

  /**
   * Sets the sort keys, most significant first. Without sort keys, the store orders hits by relevance.
   *
   * @param sort the sort keys
   * @return this query for method chaining
   */
  public SearchQuery setSort(List<SortClause> sort) {
    this.sort = sort;
    return this;
  }

  /**
   * Gets the content fields to highlight.
   *
   * @return the fields, or null when the query asks for no highlights
   */
  public List<String> getHighlightFields() {
    return highlightFields;
  }

  /**
   * Sets the content fields to highlight, as top-level or dotted nested keys. Not every store supports this.
   *
   * @param highlightFields the fields
   * @return this query for method chaining
   */
  public SearchQuery setHighlightFields(List<String> highlightFields) {
    this.highlightFields = highlightFields;
    return this;
  }
}
