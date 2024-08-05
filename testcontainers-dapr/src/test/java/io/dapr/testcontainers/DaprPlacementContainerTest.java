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

package io.dapr.testcontainers;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class DaprPlacementContainerTest {

  @ClassRule
  public static DaprPlacementContainer placement = new DaprPlacementContainer("daprio/placement");

  @Test
  public void testDaprPlacementContainerDefaults() {
    Assert.assertEquals("The default port is set", 50005, placement.getPort());
  }
}
