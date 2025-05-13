package io.dapr.it.testcontainers;

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

}
