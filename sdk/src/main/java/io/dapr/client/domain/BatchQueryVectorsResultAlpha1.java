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

import io.dapr.exceptions.DaprException;

/**
 * Outcome of one query in a batch: its matches, or the error that query failed with.
 *
 * <p>Each query in a batch succeeds or fails on its own.
 * This is an alpha API. Its name and behavior can change in a later release.
 */
public class BatchQueryVectorsResultAlpha1 {

  private final QueryVectorsResponseAlpha1 response;
  private final DaprException error;

  private BatchQueryVectorsResultAlpha1(QueryVectorsResponseAlpha1 response, DaprException error) {
    this.response = response;
    this.error = error;
  }

  /**
   * Creates the result of a query that succeeded.
   *
   * @param response the matches of the query
   * @return the result
   */
  public static BatchQueryVectorsResultAlpha1 ofResponse(QueryVectorsResponseAlpha1 response) {
    return new BatchQueryVectorsResultAlpha1(response, null);
  }

  /**
   * Creates the result of a query that failed.
   *
   * @param error why the query failed
   * @return the result
   */
  public static BatchQueryVectorsResultAlpha1 ofError(DaprException error) {
    return new BatchQueryVectorsResultAlpha1(null, error);
  }

  /**
   * Gets the matches of the query.
   *
   * @return the response, or null when the query failed
   */
  public QueryVectorsResponseAlpha1 getResponse() {
    return response;
  }

  /**
   * Gets why the query failed, with the gRPC status code, message and error details from the store.
   *
   * @return the error, or null when the query succeeded
   */
  public DaprException getError() {
    return error;
  }
}
