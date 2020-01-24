/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import okhttp3.OkHttpClient;

public class DaprHttpProxy extends io.dapr.client.DaprHttp {

  public DaprHttpProxy(int port, OkHttpClient httpClient) {
    super(port, httpClient);
  }

}
