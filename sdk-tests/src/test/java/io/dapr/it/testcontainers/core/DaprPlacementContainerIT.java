/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.it.testcontainers.core;

import io.dapr.testcontainers.DaprPlacementContainer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@Tag("testcontainers")
public class DaprPlacementContainerIT {

  @Container
  private static final DaprPlacementContainer PLACEMENT_CONTAINER =
          new DaprPlacementContainer(DAPR_PLACEMENT_IMAGE_TAG);

  @Test
  public void testDaprPlacementContainerDefaults() {
    assertEquals(50005, PLACEMENT_CONTAINER.getPort(), "The default port is not set");
  }
}
