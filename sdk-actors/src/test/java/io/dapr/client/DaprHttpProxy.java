/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import okhttp3.OkHttpClient;

public class DaprHttpProxy extends io.dapr.client.DaprHttp {

  public DaprHttpProxy(String hostname, int port, OkHttpClient httpClient) {
    super(hostname, port, httpClient);
  }

}
