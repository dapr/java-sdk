/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client.domain;

import reactor.util.context.Context;

/**
 * A Dapr Response.
 */
public class Response<T> {

  private final Context context;

  private final T object;

  public Response(Context context, T object) {
    this.context = context;
    this.object = object;
  }

  public Context getContext() {
    return this.context;
  }

  public T getObject() {
    return this.object;
  }

}
