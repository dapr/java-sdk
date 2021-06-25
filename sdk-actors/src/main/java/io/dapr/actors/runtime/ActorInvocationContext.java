/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import java.util.HashMap;
import java.util.Map;

public class ActorInvocationContext {

  private final Map<String, String> headers;

  /**
   * Constructor.
   */
  public ActorInvocationContext() {
    this.headers = new HashMap<>();
  }

  /**
   * Get the headers associated with this Actor Invocation.
   * @return the headers
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Add a header to the context.
   * @param key to reference the header with
   * @param value of the header
   */
  public void addHeader(final String key, final String value) {
    headers.put(key, value);
  }
}
