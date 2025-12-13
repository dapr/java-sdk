/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.durabletask;

import org.junit.jupiter.api.AfterEach;

import java.time.Duration;

public class IntegrationTestBase {
  protected static final Duration defaultTimeout = Duration.ofSeconds(10);

  // All tests that create a server should save it to this variable for proper shutdown
  private DurableTaskGrpcWorker server;

  @AfterEach
  public void shutdown() {
    if (this.server != null) {
      this.server.stop();
    }
  }


  protected TestDurableTaskWorkerBuilder createWorkerBuilder() {
    return new TestDurableTaskWorkerBuilder();
  }

  public class TestDurableTaskWorkerBuilder {
    final DurableTaskGrpcWorkerBuilder innerBuilder;

    private TestDurableTaskWorkerBuilder() {
      this.innerBuilder = new DurableTaskGrpcWorkerBuilder();
    }

    public DurableTaskGrpcWorker buildAndStart() {
      DurableTaskGrpcWorker server = this.innerBuilder.build();
      IntegrationTestBase.this.server = server;
      server.start();
      return server;
    }

    public TestDurableTaskWorkerBuilder setMaximumTimerInterval(Duration maximumTimerInterval) {
      this.innerBuilder.maximumTimerInterval(maximumTimerInterval);
      return this;
    }

    public TestDurableTaskWorkerBuilder addOrchestrator(
        String name,
        TaskOrchestration implementation) {
      this.innerBuilder.addOrchestration(new TaskOrchestrationFactory() {
        @Override
        public String getName() {
          return name;
        }

        @Override
        public TaskOrchestration create() {
          return implementation;
        }
      });
      return this;
    }

    public <R> TestDurableTaskWorkerBuilder addActivity(
        String name,
        TaskActivity implementation) {
      this.innerBuilder.addActivity(new TaskActivityFactory() {
        @Override
        public String getName() {
          return name;
        }

        @Override
        public TaskActivity create() {
          return implementation;
        }
      });
      return this;
    }
  }
}
