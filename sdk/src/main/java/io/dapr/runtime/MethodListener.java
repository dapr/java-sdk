/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface MethodListener {

  Mono<byte[]> process(byte[] data, Map<String, String> metadata);

}
