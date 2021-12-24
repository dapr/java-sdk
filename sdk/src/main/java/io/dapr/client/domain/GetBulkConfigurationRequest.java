package io.dapr.client.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GetBulkConfigurationRequest {
	private final String storeName;
	private final List<String> keys;
	private Map<String, String> metadata;

	public GetBulkConfigurationRequest(String storeName, List<String> keys) {
		this.storeName = storeName;
		this.keys = keys == null ? Collections.unmodifiableList(new ArrayList<>()) : Collections.unmodifiableList(keys);
	}

	public GetBulkConfigurationRequest setMetadata(Map<String, String> metadata) {
		this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
		return this;
	}

	public String getStoreName() {
		return storeName;
	}

	public List<String> getKeys() {
		return keys;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}
}
