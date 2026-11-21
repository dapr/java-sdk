/*
 * Copyright 2026 The Dapr Authors
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

import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprSecretProtos;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Secrets API calls of {@link DaprClientImpl}: get one secret and get bulk secrets.
 */
final class DaprSecretsClient {

  private final DaprGrpcCalls grpcCalls;

  DaprSecretsClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Map<String, String>> getSecret(GetSecretRequest request) {
    String secretStoreName = request.getStoreName();
    String key = request.getKey();
    Map<String, String> metadata = request.getMetadata();
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret key cannot be null or empty.");
      }
    } catch (Exception e) {
      return DaprException.wrapMono(e);
    }

    DaprSecretProtos.GetSecretRequest.Builder requestBuilder = DaprSecretProtos.GetSecretRequest.newBuilder()
        .setStoreName(secretStoreName)
        .setKey(key);

    if (metadata != null) {
      requestBuilder.putAllMetadata(metadata);
    }
    DaprSecretProtos.GetSecretRequest req = requestBuilder.build();

    return Mono.deferContextual(
        context -> grpcCalls.<DaprSecretProtos.GetSecretResponse>createMono(
            it -> grpcCalls.intercept(context).getSecret(req, it))
    ).map(DaprSecretProtos.GetSecretResponse::getDataMap);
  }

  Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request) {
    try {
      final String storeName = request.getStoreName();
      final Map<String, String> metadata = request.getMetadata();
      if ((storeName == null) || (storeName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }

      DaprSecretProtos.GetBulkSecretRequest.Builder builder = DaprSecretProtos.GetBulkSecretRequest.newBuilder()
          .setStoreName(storeName);
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }

      DaprSecretProtos.GetBulkSecretRequest envelope = builder.build();

      return Mono.deferContextual(
          context ->
              grpcCalls.<DaprSecretProtos.GetBulkSecretResponse>createMono(
                  it -> grpcCalls.intercept(context).getBulkSecret(envelope, it)
              )
      ).map(it -> {
        Map<String, DaprSecretProtos.SecretResponse> secretsMap = it.getDataMap();
        if (secretsMap == null) {
          return Collections.emptyMap();
        }
        return secretsMap
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, s -> s.getValue().getSecretsMap()));
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }
}
