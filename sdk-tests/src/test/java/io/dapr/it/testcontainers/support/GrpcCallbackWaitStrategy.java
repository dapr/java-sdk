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
 * limitations under the License.
 */
package io.dapr.it.testcontainers.support;

import com.google.protobuf.Empty;
import io.dapr.v1.AppCallbackHealthCheckGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Waits for the Dapr callback health RPC, not merely a listening socket. */
final class GrpcCallbackWaitStrategy extends AbstractWaitStrategy {
  private final int port;

  GrpcCallbackWaitStrategy(int port) {
    this.port = port;
  }

  @Override
  protected void waitUntilReady() {
    String host = waitStrategyTarget.getHost();
    int mappedPort = waitStrategyTarget.getMappedPort(port);
    try {
      awaitCallback(ManagedChannelBuilder.forAddress(host, mappedPort).usePlaintext().build(), startupTimeout);
    } catch (RuntimeException e) {
      throw new IllegalStateException("gRPC callback readiness failed for "
          + Endpoints.http(host, mappedPort).getRawAuthority() + " container=" + waitStrategyTarget.getContainerId()
          + " alias=app image=" + waitStrategyTarget.getContainerInfo().getConfig().getImage()
          + " logs:\n" + waitStrategyTarget.getLogs(), e);
    }
  }

  /** Owns and closes the supplied probe channel. */
  static void awaitCallback(ManagedChannel channel, Duration timeout) {
    long end = System.nanoTime() + timeout.toNanos();
    StatusRuntimeException last = null;
    try {
      while (System.nanoTime() < end) {
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("gRPC callback readiness interrupted");
        }
        long remaining = end - System.nanoTime();
        if (remaining <= 0) {
          break;
        }
        try {
          AppCallbackHealthCheckGrpc.newBlockingStub(channel)
              .withDeadlineAfter(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(500)), TimeUnit.NANOSECONDS)
              .healthCheck(Empty.getDefaultInstance());
          return;
        } catch (StatusRuntimeException e) {
          if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("gRPC callback readiness interrupted");
          }
          if (e.getStatus().getCode() != Status.Code.UNAVAILABLE
              && e.getStatus().getCode() != Status.Code.DEADLINE_EXCEEDED) {
            throw e;
          }
          last = e;
        }
        remaining = end - System.nanoTime();
        if (remaining > 0) {
          TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(100)));
        }
      }
      throw new IllegalStateException("Timed out waiting for gRPC callback health after " + timeout, last);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted waiting for gRPC callback health", e);
    } finally {
      channel.shutdownNow();
      boolean interrupted = Thread.interrupted();
      try {
        if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("gRPC callback probe channel did not terminate");
        }
      } catch (InterruptedException e) {
        interrupted = true;
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
