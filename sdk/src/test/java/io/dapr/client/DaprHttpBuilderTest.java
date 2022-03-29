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

package io.dapr.client;

import okhttp3.OkHttpClient;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class DaprHttpBuilderTest {

  @Test
  public void singletonOkHttpClient() throws Exception {
    DaprHttp daprHttp = new DaprHttpBuilder().build();
    DaprHttp anotherDaprHttp = new DaprHttpBuilder().build();

    assertSame(getOkHttpClient(daprHttp), getOkHttpClient(anotherDaprHttp));
  }


  private static OkHttpClient getOkHttpClient(DaprHttp daprHttp) throws Exception {
    Field httpClientField = DaprHttp.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    OkHttpClient okHttpClient = (OkHttpClient) httpClientField.get(daprHttp);
    assertNotNull(okHttpClient);
    return okHttpClient;
  }

}
