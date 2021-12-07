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

/**
 * Builder for DaprClient used in tests only.
 */
public class DaprClientTestBuilder {

    /**
     * Builds a DaprClient.
     * @param client DaprHttp used for http calls (can be mocked or stubbed)
     * @return New instance of DaprClient.
     */
    public static DaprClient buildHttpClient(DaprHttp client) {
        return new DaprClientHttp(client);
    }
}
