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
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertThrows;

public class DaprComponentTest {

  @Test
  public void componentStateStoreSerializationTest() {
    DaprContainer dapr = new DaprContainer("daprio/daprd")
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withComponent(new Component(
            "statestore",
            "state.in-memory",
            "v1",
            Collections.singletonMap("actorStateStore", "true")))
        .withAppChannelAddress("host.testcontainers.internal");

    Set<Component> components = dapr.getComponents();
    Assert.assertEquals(1, components.size());

    Component kvstore = components.iterator().next();
    Assert.assertEquals(false, kvstore.getMetadata().isEmpty());

    String componentYaml = dapr.componentToYaml(kvstore);
    String expectedComponentYaml = "metadata:\n" + "  name: statestore\n"
        + "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Component\n"
        + "spec:\n"
        + "  metadata:\n"
        + "  - name: actorStateStore\n"
        + "    value: 'true'\n"
        + "  type: state.in-memory\n"
        + "  version: v1\n";

    Assert.assertEquals(expectedComponentYaml, componentYaml);
  }

  @Test
  public void containerConfigurationTest() {
    DaprContainer dapr = new DaprContainer("daprio/daprd")
            .withAppName("dapr-app")
            .withAppPort(8081)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withAppChannelAddress("host.testcontainers.internal");

    dapr.configure();

    assertThrows(IllegalStateException.class, () -> { dapr.getHttpEndpoint(); });
    assertThrows(IllegalStateException.class, () -> { dapr.getGrpcPort(); });




  }

  @Test
  public void subscriptionSerializationTest() {
    DaprContainer dapr = new DaprContainer("daprio/daprd")
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withSubscription(new Subscription("my-subscription", "pubsub", "topic", "/events"))
        .withAppChannelAddress("host.testcontainers.internal");

    Set<Subscription> subscriptions = dapr.getSubscriptions();
    Assert.assertEquals(1, subscriptions.size());

    String subscriptionYaml = dapr.subscriptionToYaml(subscriptions.iterator().next());
    String expectedSubscriptionYaml = "metadata:\n" + "  name: my-subscription\n"
        + "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Subscription\n"
        + "spec:\n"
        + "  route: /events\n"
        + "  pubsubname: pubsub\n"
        + "  topic: topic\n";
    Assert.assertEquals(expectedSubscriptionYaml, subscriptionYaml);
  }

  @Test
  public void withComponentFromPath() {
    URL stateStoreYaml = this.getClass().getClassLoader().getResource("dapr-resources/statestore.yaml");
    Path path = Paths.get(stateStoreYaml.getPath());

    DaprContainer dapr = new DaprContainer("daprio/daprd")
        .withAppName("dapr-app")
        .withAppPort(8081)
        .withComponent(path)
        .withAppChannelAddress("host.testcontainers.internal");

    Set<Component> components = dapr.getComponents();
    Assert.assertEquals(1, components.size());
    Component kvstore = components.iterator().next();
    Assert.assertEquals(false, kvstore.getMetadata().isEmpty());

    String componentYaml = dapr.componentToYaml(kvstore);
    String expectedComponentYaml = "metadata:\n"
        + "  name: statestore\n"
        + "apiVersion: dapr.io/v1alpha1\n"
        + "kind: Component\n"
        + "spec:\n"
        + "  metadata:\n"
        + "  - name: name\n"
        + "    value: keyPrefix\n"
        + "  - name: value\n"
        + "    value: name\n"
        + "  - name: name\n"
        + "    value: redisHost\n"
        + "  - name: value\n"
        + "    value: redis:6379\n"
        + "  - name: name\n"
        + "    value: redisPassword\n"
        + "  - name: value\n"
        + "    value: ''\n"
        + "  type: null\n"
        + "  version: v1\n";

    Assert.assertEquals(expectedComponentYaml, componentYaml);
  }
}
