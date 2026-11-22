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

import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprLockProtos;
import reactor.core.publisher.Mono;

/**
 * Distributed lock API calls of {@link DaprClientImpl}: try lock and unlock (alpha).
 */
final class DaprLockClient {

  private final DaprGrpcCalls grpcCalls;

  DaprLockClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Boolean> tryLock(LockRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final String resourceId = request.getResourceId();
      final String lockOwner = request.getLockOwner();
      final Integer expiryInSeconds = request.getExpiryInSeconds();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (resourceId == null || resourceId.isEmpty()) {
        throw new IllegalArgumentException("ResourceId cannot be null or empty.");
      }
      if (lockOwner == null || lockOwner.isEmpty()) {
        throw new IllegalArgumentException("LockOwner cannot be null or empty.");
      }
      if (expiryInSeconds < 0) {
        throw new IllegalArgumentException("ExpiryInSeconds cannot be negative.");
      }

      DaprLockProtos.TryLockRequest.Builder builder = DaprLockProtos.TryLockRequest.newBuilder()
              .setStoreName(stateStoreName)
              .setResourceId(resourceId)
              .setLockOwner(lockOwner)
              .setExpiryInSeconds(expiryInSeconds);

      DaprLockProtos.TryLockRequest tryLockRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprLockProtos.TryLockResponse>createMono(
                      it -> grpcCalls.intercept(context).tryLockAlpha1(tryLockRequest, it)
              )
      ).flatMap(response -> {
        try {
          return Mono.just(response.getSuccess());
        } catch (Exception ex) {
          return DaprException.wrapMono(ex);
        }
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<UnlockResponseStatus> unlock(UnlockRequest request) {
    try {
      final String stateStoreName = request.getStoreName();
      final String resourceId = request.getResourceId();
      final String lockOwner = request.getLockOwner();

      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if (resourceId == null || resourceId.isEmpty()) {
        throw new IllegalArgumentException("ResourceId cannot be null or empty.");
      }
      if (lockOwner == null || lockOwner.isEmpty()) {
        throw new IllegalArgumentException("LockOwner cannot be null or empty.");
      }

      DaprLockProtos.UnlockRequest.Builder builder = DaprLockProtos.UnlockRequest.newBuilder()
              .setStoreName(stateStoreName)
              .setResourceId(resourceId)
              .setLockOwner(lockOwner);

      DaprLockProtos.UnlockRequest unlockRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprLockProtos.UnlockResponse>createMono(
                      it -> grpcCalls.intercept(context).unlockAlpha1(unlockRequest, it)
              )
      ).flatMap(response -> {
        try {
          return Mono.just(UnlockResponseStatus.valueOf(response.getStatus().getNumber()));
        } catch (Exception ex) {
          return DaprException.wrapMono(ex);
        }
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }
}
