package io.dapr.client.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GetConfigurationRequest {
	private final String storeName;
	private final String key;
	private Map<String, String> metadata;

	public GetConfigurationRequest(String storeName, String key) {
		this.storeName = storeName;
		this.key = key;
	}

	public GetConfigurationRequest setMetadata(Map<String, String> metadata) {
		this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
		return this;
	}

	public String getStoreName() {
		return storeName;
	}

	public String getKey() {
		return key;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}
}
