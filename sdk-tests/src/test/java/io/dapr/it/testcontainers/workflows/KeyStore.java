/*
 * Copyright 2025 The Dapr Authors
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
package io.dapr.it.testcontainers.workflows;

import java.util.HashMap;
import java.util.Map;

public class KeyStore {

    private final Map<String, Boolean> keyStore = new HashMap<>();

    private static KeyStore instance;

    private KeyStore() {
    }

    public static KeyStore getInstance() {
        if (instance == null) {
            synchronized (KeyStore.class) {
                if (instance == null) {
                    instance = new KeyStore();
                }
            }
        }
        return instance;
    }
    

    public void addKey(String key, Boolean value) {
        keyStore.put(key, value);
    }

    public Boolean getKey(String key) {
        return keyStore.get(key);
    }

    public void removeKey(String key) {
        keyStore.remove(key);
    }

    public int size() {
        return keyStore.size();
    }

}
