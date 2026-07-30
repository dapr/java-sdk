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

package io.dapr.workflows.internal;

import com.google.protobuf.Empty;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application-level keepalive for a gRPC channel to the Dapr sidecar.
 *
 * <p>Periodically invokes the sidecar's {@code hello} RPC so that intermediaries
 * (e.g. AWS ALBs) that do not treat HTTP/2 PING frames as connection activity
 * never see the connection as idle and close it.</p>
 */
public final class GrpcChannelKeepalive implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcChannelKeepalive.class);
  private static final long KEEPALIVE_DEADLINE_SECONDS = 5;

  private final TaskHubSidecarServiceGrpc.TaskHubSidecarServiceBlockingStub stub;
  private final String threadName;
  private final Duration interval;
  private ScheduledExecutorService scheduler;

  /**
   * Creates a keepalive for the given channel. No pings are sent until {@link #start()}.
   *
   * @param channel    channel to the Dapr sidecar to keep alive.
   * @param threadName name of the keepalive thread, to tell instances apart in thread dumps.
   * @param interval   delay between pings.
   */
  public GrpcChannelKeepalive(Channel channel, String threadName, Duration interval) {
    this.stub = TaskHubSidecarServiceGrpc.newBlockingStub(channel);
    this.threadName = threadName;
    this.interval = interval;
  }

  /**
   * Starts the keepalive loop. Does nothing if already started.
   */
  public synchronized void start() {
    if (this.scheduler != null) {
      return;
    }
    ScheduledExecutorService newScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, this.threadName);
      thread.setDaemon(true);
      return thread;
    });
    // The deadline must be applied per ping: Deadline.after snapshots the clock when
    // withDeadlineAfter is invoked, so hoisting it onto the cached stub would make
    // every ping after the first fail immediately with DEADLINE_EXCEEDED.
    // Catch Throwable, not just RuntimeException: any throwable escaping a periodic
    // task silently cancels all future runs.
    newScheduler.scheduleWithFixedDelay(() -> {
      try {
        this.stub.withDeadlineAfter(KEEPALIVE_DEADLINE_SECONDS, TimeUnit.SECONDS)
            .hello(Empty.getDefaultInstance());
      } catch (Throwable e) {
        LOGGER.debug("Sidecar keepalive ping failed", e);
      }
    }, this.interval.toMillis(), this.interval.toMillis(), TimeUnit.MILLISECONDS);
    this.scheduler = newScheduler;
  }

  /**
   * Stops the keepalive loop. Does nothing if not started.
   */
  @Override
  public synchronized void close() {
    if (this.scheduler != null) {
      this.scheduler.shutdownNow();
      this.scheduler = null;
    }
  }
}
