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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DaprContainer extends GenericContainer<DaprContainer> {

  private static final Logger log = LoggerFactory.getLogger(DaprContainer.class);
  private static final int DAPRD_DEFAULT_HTTP_PORT = 3500;
  private static final int DAPRD_DEFAULT_GRPC_PORT = 50001;
  private static final WaitStrategy WAIT_STRATEGY = Wait.forHttp("/v1.0/healthz/outbound")
      .forPort(DAPRD_DEFAULT_HTTP_PORT)
      .forStatusCodeMatching(statusCode -> statusCode >= 200 && statusCode <= 399);

  private final Set<Component> components = new HashSet<>();
  private Configuration configuration;
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
    withAccessToHost(true);
    withExposedPorts(DAPRD_DEFAULT_HTTP_PORT, DAPRD_DEFAULT_GRPC_PORT);
    setWaitStrategy(WAIT_STRATEGY);
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

  public Configuration getConfiguration() {
    return configuration;
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

  public DaprContainer withConfiguration(Configuration configuration) {
    this.configuration = configuration;
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

  public String getGrpcEndpoint() {
    return ":" + getMappedPort(DAPRD_DEFAULT_GRPC_PORT);
  }

  public DaprContainer withAppChannelAddress(String appChannelAddress) {
    this.appChannelAddress = appChannelAddress;
    return this;
  }


  /**
   * Get a map of Dapr component details.
   * @param configuration A Dapr Configuration.
   * @return Map of component details.
   */
  public Map<String, Object> configurationToMap(Configuration configuration) {
    Map<String, Object> configurationProps = new HashMap<>();
    configurationProps.put("apiVersion", "dapr.io/v1alpha1");
    configurationProps.put("kind", "Configuration");

    Map<String, String> configurationMetadata = new LinkedHashMap<>();
    configurationMetadata.put("name", configuration.getName());
    configurationProps.put("metadata", configurationMetadata);

    Map<String, Object> configurationSpec = new HashMap<>();


    Map<String, Object> configurationTracing = new HashMap<>();
    Map<String, Object> configurationTracingOtel = new HashMap<>();
    if (configuration.getTracing() != null) {
      configurationTracing.put("samplingRate", configuration.getTracing().getSamplingRate());
      configurationTracing.put("stdout", configuration.getTracing().getStdout());
      configurationTracingOtel.put("endpointAddress", configuration.getTracing().getOtelEndpoint());
      configurationTracingOtel.put("isSecure", configuration.getTracing().getOtelIsSecure());
      configurationTracingOtel.put("protocol", configuration.getTracing().getOtelProtocol());
    }

    configurationTracing.put("otel", configurationTracingOtel);
    configurationSpec.put("tracing", configurationTracing);

    configurationProps.put("spec", configurationSpec);
    return Collections.unmodifiableMap(configurationProps);
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

    if (configuration != null) {
      cmds.add("--config");
      cmds.add("/dapr-resources/" + configuration.getName() + ".yaml");
    }

    cmds.add("--log-level");
    cmds.add(daprLogLevel.toString());
    cmds.add("--resources-path");
    cmds.add("/dapr-resources");

    String[] cmdArray = cmds.toArray(new String[]{});
    log.info("> `daprd` Command: \n");
    log.info("\t" + Arrays.toString(cmdArray) + "\n");

    withCommand(cmdArray);

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

    if (configuration != null) {
      String configurationYaml = configurationToYaml(configuration);
      withCopyToContainer(Transferable.of(configurationYaml), "/dapr-resources/" + configuration.getName() + ".yaml");
    }

    for (Subscription subscription : subscriptions) {
      String subscriptionYaml = subscriptionToYaml(subscription);
      withCopyToContainer(Transferable.of(subscriptionYaml), "/dapr-resources/" + subscription.getName() + ".yaml");
    }

    dependsOn(placementContainer);
  }

  /**
   * Get a Yaml representation of a Subscription.
   * @param subscription A Dapr Subscription.
   * @return String representing the Subscription in Yaml format
   */
  public String subscriptionToYaml(Subscription subscription) {
    Map<String, Object> subscriptionMap = subscriptionToMap(subscription);
    String subscriptionYaml = yaml.dumpAsMap(subscriptionMap);
    log.info("> Subscription YAML: \n");
    log.info("\t\n" + subscriptionYaml + "\n");
    return subscriptionYaml;
  }

  /**
   * Get a Yaml representation of a Component.
   * @param component A Dapr Subscription.
   * @return String representing the Component in Yaml format
   */
  public String componentToYaml(Component component) {
    Map<String, Object> componentMap = componentToMap(component);
    String componentYaml = yaml.dumpAsMap(componentMap);
    log.info("> Component YAML: \n");
    log.info("\t\n" + componentYaml + "\n");
    return componentYaml;
  }

  /**
   * Get a Yaml representation of a Configuration.
   * @param configuration A Dapr Subscription.
   * @return String representing the Configuration in Yaml format
   */
  public String configurationToYaml(Configuration configuration) {
    Map<String, Object> configurationMap = configurationToMap(configuration);
    String configurationYaml = yaml.dumpAsMap(configurationMap);
    log.info("> Configuration YAML: \n");
    log.info("\t\n" + configurationYaml + "\n");
    return configurationYaml;
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

  private static Yaml getYamlMapper() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Representer representer = new Representer(options);
    representer.addClassTag(MetadataEntry.class, Tag.MAP);
    return new Yaml(representer);
  }
}
