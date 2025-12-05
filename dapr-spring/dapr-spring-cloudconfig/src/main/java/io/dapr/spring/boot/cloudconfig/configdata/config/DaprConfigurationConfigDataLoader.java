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

package io.dapr.spring.boot.cloudconfig.configdata.config;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigClientManager;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigProperties;
import io.dapr.spring.boot.cloudconfig.configdata.DaprCloudConfigParserHandler;
import org.apache.commons.logging.Log;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_IMPORTS;
import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_PROFILES;
import static org.springframework.boot.context.config.ConfigData.Option.PROFILE_SPECIFIC;

public class DaprConfigurationConfigDataLoader implements ConfigDataLoader<DaprConfigurationConfigDataResource> {

  private final Log log;

  private DaprClient daprClient;

  private DaprCloudConfigProperties daprCloudConfigProperties;

  /**
   * Create a Config Data Loader to load config from Dapr Configuration api.
   *
   * @param logFactory            logFactory
   * @param daprClient            Dapr Client created
   * @param daprCloudConfigProperties Dapr Cloud Config Properties
   */
  public DaprConfigurationConfigDataLoader(DeferredLogFactory logFactory, DaprClient daprClient,
                                           DaprCloudConfigProperties daprCloudConfigProperties) {
    this.log = logFactory.getLog(getClass());
    this.daprClient = daprClient;
    this.daprCloudConfigProperties = daprCloudConfigProperties;
  }


  /**
   * Load {@link ConfigData} for the given resource.
   *
   * @param context  the loader context
   * @param resource the resource to load
   * @return the loaded config data or {@code null} if the location should be skipped
   * @throws IOException                         on IO error
   * @throws ConfigDataResourceNotFoundException if the resource cannot be found
   */
  @Override
  public ConfigData load(ConfigDataLoaderContext context, DaprConfigurationConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {
    DaprCloudConfigClientManager daprClientSecretStoreConfigManager =
        getBean(context, DaprCloudConfigClientManager.class);

    daprClient = DaprCloudConfigClientManager.getDaprClient();
    daprCloudConfigProperties = daprClientSecretStoreConfigManager.getDaprCloudConfigProperties();

    if (!daprCloudConfigProperties.getEnabled()) {
      return ConfigData.EMPTY;
    }

    if (daprCloudConfigProperties.getWaitSidecarEnabled()) {
      waitForSidecar();
    }

    return fetchConfig(resource);
  }

  private void waitForSidecar() throws IOException {
    try {
      daprClient.waitForSidecar(daprCloudConfigProperties.getTimeout())
          .retry(daprCloudConfigProperties.getWaitSidecarRetries())
          .block();
    } catch (RuntimeException e) {
      log.info(e.getMessage(), e);
      throw new IOException("Failed to wait for sidecar", e);
    }
  }

  private ConfigData fetchConfig(DaprConfigurationConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {
    Mono<Map<String, ConfigurationItem>> secretMapMono = daprClient.getConfiguration(new GetConfigurationRequest(
        resource.getStoreName(),
        StringUtils.hasText(resource.getConfigName())
            ? List.of(resource.getConfigName())
            : null
    ));

    try {
      Map<String, ConfigurationItem> secretMap =
          secretMapMono.block(Duration.ofMillis(daprCloudConfigProperties.getTimeout()));

      if (secretMap == null) {
        log.info("Config not found");
        throw new ConfigDataResourceNotFoundException(resource);
      }

      Map<String, String> configMap = new HashMap<>();
      secretMap.forEach((key, value) -> {
        configMap.put(value.getKey(), value.getValue());
      });

      List<PropertySource<?>> sourceList =
          new ArrayList<>(DaprCloudConfigParserHandler.getInstance().parseDaprCloudConfigData(
              resource.getStoreName(),
              configMap,
              resource.getType()
          ));

      return new ConfigData(sourceList, IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
    } catch (RuntimeException e) {
      log.info("Failed to get config from sidecar: " + e.getMessage(), e);
      throw new IOException("Failed to get config from sidecar", e);
    }

  }

  protected <T> T getBean(ConfigDataLoaderContext context, Class<T> type) {
    if (context.getBootstrapContext().isRegistered(type)) {
      return context.getBootstrapContext().get(type);
    }
    return null;
  }
}
