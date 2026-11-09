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

package io.dapr.workflows.task.orchestration;

import io.dapr.workflows.task.TaskOrchestration;
import io.dapr.workflows.task.exception.VersionNotRegisteredException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskOrchestrationFactoriesTest {

  private final TaskOrchestrationFactories factories = new TaskOrchestrationFactories();

  private static TaskOrchestrationFactory factory(String name, String version, boolean latest) {
    return new TaskOrchestrationFactory() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public TaskOrchestration create() {
        return ctx -> ctx.complete(null);
      }

      @Override
      public String getVersionName() {
        return version;
      }

      @Override
      public Boolean isLatestVersion() {
        return latest;
      }
    };
  }

  @Test
  public void shouldReturnAnUnversionedFactoryByName() {
    TaskOrchestrationFactory registered = factory("DemoWorkflow", "", false);

    factories.addOrchestration(registered);

    assertSame(registered, factories.getOrchestrationFactory("DemoWorkflow"));
    assertSame(registered, factories.getOrchestrationFactory("DemoWorkflow", "v1"),
        "an unversioned registration answers for every version");
  }

  @Test
  public void shouldRejectAFactoryWithNoName() {
    assertThrows(IllegalArgumentException.class, () -> factories.addOrchestration(factory(null, "", false)));
    assertThrows(IllegalArgumentException.class, () -> factories.addOrchestration(factory("", "", false)));
  }

  @Test
  public void shouldRejectASecondUnversionedRegistrationOfTheSameName() {
    factories.addOrchestration(factory("DemoWorkflow", "", false));

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> factories.addOrchestration(factory("DemoWorkflow", "", false)));

    assertTrue(thrown.getMessage().contains("already registered"), thrown.getMessage());
  }

  @Test
  public void shouldReturnAVersionedFactoryByNameAndVersion() {
    TaskOrchestrationFactory v1 = factory("DemoWorkflow", "v1", false);
    TaskOrchestrationFactory v2 = factory("DemoWorkflow", "v2", false);

    factories.addOrchestration(v1);
    factories.addOrchestration(v2);

    assertSame(v1, factories.getOrchestrationFactory("DemoWorkflow", "v1"));
    assertSame(v2, factories.getOrchestrationFactory("DemoWorkflow", "v2"));
  }

  @Test
  public void shouldRejectTheSameVersionTwice() {
    factories.addOrchestration(factory("DemoWorkflow", "v1", false));

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> factories.addOrchestration(factory("DemoWorkflow", "v1", false)));

    assertTrue(thrown.getMessage().contains("v1"), thrown.getMessage());
  }

  @Test
  public void shouldFallBackToTheLatestVersionWhenNoneIsAsked() {
    TaskOrchestrationFactory v1 = factory("DemoWorkflow", "v1", false);
    TaskOrchestrationFactory v2 = factory("DemoWorkflow", "v2", true);

    factories.addOrchestration(v1);
    factories.addOrchestration(v2);

    assertSame(v2, factories.getOrchestrationFactory("DemoWorkflow"));
  }

  @Test
  public void shouldRefuseASecondLatestVersion() {
    factories.addOrchestration(factory("DemoWorkflow", "v1", true));

    assertThrows(IllegalStateException.class,
        () -> factories.addOrchestration(factory("DemoWorkflow", "v2", true)));
  }

  @Test
  public void shouldReturnNullForANameThatWasNeverRegistered() {
    assertNull(factories.getOrchestrationFactory("Unknown"));
    assertNull(factories.getOrchestrationFactory("Unknown", "v1"));
  }

  @Test
  public void shouldRejectAVersionThatWasNeverRegistered() {
    factories.addOrchestration(factory("DemoWorkflow", "v1", false));

    assertThrows(VersionNotRegisteredException.class,
        () -> factories.getOrchestrationFactory("DemoWorkflow", "v2"));
  }
}
