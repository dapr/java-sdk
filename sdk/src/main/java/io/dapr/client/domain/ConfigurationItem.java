package io.dapr.client.domain;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationItem {
    private final String key;
    private final String value;
    private final String version;
    private final Map<String, String> metadata;

    public ConfigurationItem(String key, String value, String version, Map<String, String> metadata) {
        this.key = key;
        this.value = value;
        this.version = version;
        this.metadata = metadata;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
