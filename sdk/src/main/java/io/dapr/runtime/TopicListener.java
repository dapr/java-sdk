/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface TopicListener {

  Mono<Void> process(String messageId, String dataType, byte[] data, Map<String, String> metadata);

}
