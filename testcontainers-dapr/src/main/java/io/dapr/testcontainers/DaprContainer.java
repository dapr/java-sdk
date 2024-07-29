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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DaprContainer extends GenericContainer<DaprContainer> {

  private static final int DAPRD_DEFAULT_HTTP_PORT = 3500;
  private static final int DAPRD_DEFAULT_GRPC_PORT = 50001;
  private final Set<Component> components = new HashSet<>();
  private final Set<Subscription> subscriptions = new HashSet<>();
  private DaprProtocol protocol = DaprProtocol.HTTP;
  private String appName;
  private Integer appPort = null;
  private DaprLogLevel daprLogLevel = DaprLogLevel.INFO;
  private String appChannelAddress = "localhost";
  private String placementService = "placement";
  private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("daprio/daprd");
  private static final Yaml yaml = getYamlMapper();
  private DaprPlacementContainer placementContainer;
  private String placementDockerImageName = "daprio/placement";
  private boolean shouldReusePlacement;

  /**
   * Creates a new Dapr container.
   * @param dockerImageName Docker image name.
   */
  public DaprContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
    dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    // For susbcriptions the container needs to access the app channel
    withAccessToHost(true);
    // Here we don't want to wait for the Dapr sidecar to be ready, as the sidecar
    // needs to
    // connect with the application for susbcriptions

    withExposedPorts(DAPRD_DEFAULT_HTTP_PORT, DAPRD_DEFAULT_GRPC_PORT);

  }

  private static Yaml getYamlMapper() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Representer representer = new Representer(options);
    representer.addClassTag(MetadataEntry.class, Tag.MAP);
    return new Yaml(representer);
  }

  /**
   * Creates a new Dapr container.
   * @param image Docker image name.
   */
  public DaprContainer(String image) {
    this(DockerImageName.parse(image));
  }

  public Set<Component> getComponents() {
    return components;
  }

  public Set<Subscription> getSubscriptions() {
    return subscriptions;
  }

  public DaprContainer withAppPort(Integer port) {
    this.appPort = port;
    return this;
  }

  public DaprContainer withPlacementService(String placementService) {
    this.placementService = placementService;
    return this;
  }

  public DaprContainer withAppName(String appName) {
    this.appName = appName;
    return this;
  }

  public DaprContainer withDaprLogLevel(DaprLogLevel daprLogLevel) {
    this.daprLogLevel = daprLogLevel;
    return this;
  }

  public DaprContainer withSubscription(Subscription subscription) {
    subscriptions.add(subscription);
    return this;
  }

  public DaprContainer withComponent(Component component) {
    components.add(component);
    return this;
  }



  /**
   * Adds a Dapr component from a YAML file.
   * @param path Path to the YAML file.
   * @return This container.
   */
  public DaprContainer withComponent(Path path) {
    try {
      Map<String, Object> component = this.yaml.loadAs(Files.newInputStream(path), Map.class);

      String type = (String) component.get("type");
      Map<String, Object> metadata = (Map<String, Object>) component.get("metadata");
      String name = (String) metadata.get("name");

      Map<String, Object> spec = (Map<String, Object>) component.get("spec");
      String version = (String) spec.get("version");
      List<Map<String, String>> specMetadata =
          (List<Map<String, String>>) spec.getOrDefault("metadata", Collections.emptyMap());

      ArrayList<MetadataEntry> metadataEntries = new ArrayList<>();

      for (Map<String, String> specMetadataItem : specMetadata) {
        for (Map.Entry<String, String> metadataItem : specMetadataItem.entrySet()) {
          metadataEntries.add(new MetadataEntry(metadataItem.getKey(), metadataItem.getValue()));
        }
      }

      return withComponent(new Component(name, type, version, metadataEntries));
    } catch (IOException e) {
      logger().warn("Error while reading component from {}", path.toAbsolutePath());
    }
    return this;
  }

  public int getHttpPort() {
    return getMappedPort(DAPRD_DEFAULT_HTTP_PORT);
  }

  public String getHttpEndpoint() {
    return "http://" + getHost() + ":" + getMappedPort(DAPRD_DEFAULT_HTTP_PORT);
  }

  public int getGrpcPort() {
    return getMappedPort(DAPRD_DEFAULT_GRPC_PORT);
  }

  public DaprContainer withAppChannelAddress(String appChannelAddress) {
    this.appChannelAddress = appChannelAddress;
    return this;
  }

  /**
   * Get a map of Dapr component details.
   * @param component A Dapr Component.
   * @return Map of component details.
   */
  public Map<String, Object> componentToMap(Component component) {
    Map<String, Object> componentProps = new HashMap<>();
    componentProps.put("apiVersion", "dapr.io/v1alpha1");
    componentProps.put("kind", "Component");

    Map<String, String> componentMetadata = new LinkedHashMap<>();
    componentMetadata.put("name", component.getName());
    componentProps.put("metadata", componentMetadata);

    Map<String, Object> componentSpec = new HashMap<>();
    componentSpec.put("type", component.getType());
    componentSpec.put("version", component.getVersion());

    if (!component.getMetadata().isEmpty()) {
      componentSpec.put("metadata", component.getMetadata());
    }
    componentProps.put("spec", componentSpec);
    return Collections.unmodifiableMap(componentProps);
  }

  /**
   * Get a map of Dapr subscription details.
   * @param subscription A Dapr Subscription.
   * @return Map of subscription details.
   */
  public Map<String, Object> subscriptionToMap(Subscription subscription) {
    Map<String, Object> subscriptionProps = new HashMap<>();
    subscriptionProps.put("apiVersion", "dapr.io/v1alpha1");
    subscriptionProps.put("kind", "Subscription");

    Map<String, String> subscriptionMetadata = new LinkedHashMap<>();
    subscriptionMetadata.put("name", subscription.getName());
    subscriptionProps.put("metadata", subscriptionMetadata);

    Map<String, Object> subscriptionSpec = new HashMap<>();
    subscriptionSpec.put("pubsubname", subscription.getPubsubName());
    subscriptionSpec.put("topic", subscription.getTopic());
    subscriptionSpec.put("route", subscription.getRoute());

    subscriptionProps.put("spec", subscriptionSpec);
    return Collections.unmodifiableMap(subscriptionProps);
  }

  @Override
  protected void configure() {
    super.configure();

    if (getNetwork() == null) {
      withNetwork(Network.newNetwork());
    }
    if (this.placementContainer == null) {
      this.placementContainer = new DaprPlacementContainer(this.placementDockerImageName)
          .withNetwork(getNetwork())
          .withNetworkAliases(placementService)
          .withReuse(this.shouldReusePlacement);
      this.placementContainer.start();
    }

    List<String> cmds = new ArrayList<>();
    cmds.add("./daprd");
    cmds.add("-app-id");
    cmds.add(appName);
    cmds.add("--dapr-listen-addresses=0.0.0.0");
    cmds.add("--app-protocol");
    cmds.add(protocol.getName());
    cmds.add("-placement-host-address");
    cmds.add(placementService + ":50005");

    if (appChannelAddress != null && !appChannelAddress.isEmpty()) {
      cmds.add("--app-channel-address");
      cmds.add(appChannelAddress);
    }
    if (appPort != null) {
      cmds.add("--app-port");
      cmds.add(Integer.toString(appPort));
    }
    cmds.add("--log-level");
    cmds.add(daprLogLevel.toString());
    cmds.add("-components-path");
    cmds.add("/dapr-resources");
    withCommand(cmds.toArray(new String[]{}));

    if (components.isEmpty()) {
      components.add(new Component("kvstore", "state.in-memory", "v1", Collections.emptyMap()));
      components.add(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()));
    }

    if (subscriptions.isEmpty() && !components.isEmpty()) {
      subscriptions.add(new Subscription("local", "pubsub", "topic", "/events"));
    }

    for (Component component : components) {
      String componentYaml = componentToYaml(component);
      withCopyToContainer(Transferable.of(componentYaml), "/dapr-resources/" + component.getName() + ".yaml");
    }

    for (Subscription subscription : subscriptions) {
      String subscriptionYaml = subscriptionToYaml(subscription);
      withCopyToContainer(Transferable.of(subscriptionYaml), "/dapr-resources/" + subscription.getName() + ".yaml");
    }
  }

  public String subscriptionToYaml(Subscription subscription) {
    Map<String, Object> subscriptionMap = subscriptionToMap(subscription);
    return yaml.dumpAsMap(subscriptionMap);
  }

  public String componentToYaml(Component component) {
    Map<String, Object> componentMap = componentToMap(component);
    return yaml.dumpAsMap(componentMap);
  }

  public String getAppName() {
    return appName;
  }

  public Integer getAppPort() {
    return appPort;
  }

  public String getAppChannelAddress() {
    return appChannelAddress;
  }

  public String getPlacementService() {
    return placementService;
  }

  public static DockerImageName getDefaultImageName() {
    return DEFAULT_IMAGE_NAME;
  }

  public DaprContainer withPlacementImage(String placementDockerImageName) {
    this.placementDockerImageName = placementDockerImageName;
    return this;
  }

  public DaprContainer withReusablePlacement(boolean reuse) {
    this.shouldReusePlacement = reuse;
    return this;
  }

  public DaprContainer withPlacementContainer(DaprPlacementContainer placementContainer) {
    this.placementContainer = placementContainer;
    return this;
  }

  // Required by spotbugs plugin
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
