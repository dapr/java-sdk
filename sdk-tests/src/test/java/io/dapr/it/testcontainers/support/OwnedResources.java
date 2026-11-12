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

import org.testcontainers.lifecycle.Startable;

import java.util.ArrayList;
import java.util.List;

/** Starts in order and always stops attempted resources in reverse order. */
final class OwnedResources {
  private final List<Startable> attempted = new ArrayList<>();
  private final AutoCloseable network;
  private boolean closed;

  OwnedResources(AutoCloseable network) {
    this.network = network;
  }

  void start(Startable resource) {
    if (closed) {
      throw new IllegalStateException("Environment is already closed");
    }
    attempted.add(resource);
    resource.start();
  }

  void stop() {
    if (closed) {
      return;
    }
    closed = true;
    Throwable failure = null;
    for (int i = attempted.size() - 1; i >= 0; i--) {
      try {
        attempted.get(i).stop();
      } catch (RuntimeException | Error e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    try {
      network.close();
    } catch (Exception | Error e) {
      if (failure == null) {
        failure = new IllegalStateException("Could not close network", e);
      } else {
        failure.addSuppressed(e);
      }
    }
    if (failure instanceof Error) {
      throw (Error) failure;
    }
    if (failure != null) {
      throw (RuntimeException) failure;
    }
  }
}
