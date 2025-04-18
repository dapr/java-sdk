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

package io.dapr.spring.boot.cloudconfig.configdata.secret;

import io.dapr.client.DaprClient;
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
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_IMPORTS;
import static org.springframework.boot.context.config.ConfigData.Option.IGNORE_PROFILES;
import static org.springframework.boot.context.config.ConfigData.Option.PROFILE_SPECIFIC;

public class DaprSecretStoreConfigDataLoader implements ConfigDataLoader<DaprSecretStoreConfigDataResource> {

  private final Log log;

  private DaprClient daprClient;

  private DaprCloudConfigProperties daprCloudConfigProperties;

  /**
   * Create a Config Data Loader to load config from Dapr Secret Store api.
   *
   * @param logFactory logFactory
   * @param daprClient Dapr Client created
   * @param daprCloudConfigProperties Dapr Cloud Config Properties
   */
  public DaprSecretStoreConfigDataLoader(DeferredLogFactory logFactory, DaprClient daprClient,
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
  public ConfigData load(ConfigDataLoaderContext context, DaprSecretStoreConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {
    DaprCloudConfigClientManager daprCloudConfigClientManager =
        getBean(context, DaprCloudConfigClientManager.class);

    daprClient = DaprCloudConfigClientManager.getDaprClient();
    daprCloudConfigProperties = daprCloudConfigClientManager.getDaprCloudConfigProperties();

    if (!daprCloudConfigProperties.getEnabled()) {
      return ConfigData.EMPTY;
    }

    if (daprCloudConfigProperties.getWaitSidecarEnabled()) {
      waitForSidecar();
    }

    if (resource.getSecretName() == null) {
      return fetchBulkSecret(resource);
    } else {
      return fetchSecret(resource);
    }
  }

  private void waitForSidecar() throws IOException {
    try {
      daprClient.waitForSidecar(daprCloudConfigProperties.getTimeout())
          .retry(daprCloudConfigProperties.getWaitSidecarRetries())
          .block();
    } catch (RuntimeException e) {
      log.info("Failed to wait for sidecar: " + e.getMessage(), e);
      throw new IOException("Failed to wait for sidecar", e);
    }
  }

  /**
   * Get Bulk Secret from Store.
   * @param resource Secret Data Resource to fetch
   * @return config data
   * @throws IOException for block returns exception
   * @throws ConfigDataResourceNotFoundException for secret not found
   */
  private ConfigData fetchBulkSecret(DaprSecretStoreConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {

    Mono<Map<String, Map<String, String>>> secretMapMono = daprClient.getBulkSecret(resource.getStoreName());

    try {
      Map<String, Map<String, String>> secretMap =
          secretMapMono.block(Duration.ofMillis(daprCloudConfigProperties.getTimeout()));

      if (secretMap == null) {
        log.info("Secret not found");
        throw new ConfigDataResourceNotFoundException(resource);
      }

      List<PropertySource<?>> sourceList = new ArrayList<>();

      for (Map.Entry<String, Map<String, String>> entry : secretMap.entrySet()) {
        sourceList.addAll(DaprCloudConfigParserHandler.getInstance().parseDaprCloudConfigData(
            resource.getStoreName() + ":" + entry.getKey(),
            entry.getValue(),
            resource.getType()
        ));
      }

      log.debug(String.format("now gain %d data source in secret, storename = %s",
          sourceList.size(), resource.getStoreName()));
      return new ConfigData(sourceList, IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
    } catch (RuntimeException e) {
      log.info("Failed to get secret from sidecar: " + e.getMessage(), e);
      throw new IOException("Failed to get secret from sidecar", e);
    }
  }

  /**
   * Get Secret from Store.
   * @param resource Secret Data Resource to fetch
   * @return config data
   * @throws IOException for block returns exception
   * @throws ConfigDataResourceNotFoundException for secret not found
   */
  private ConfigData fetchSecret(DaprSecretStoreConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {
    Mono<Map<String, String>> secretMapMono = daprClient.getSecret(resource.getStoreName(), resource.getSecretName());

    try {
      Map<String, String> secretMap = secretMapMono.block(Duration.ofMillis(daprCloudConfigProperties.getTimeout()));

      if (secretMap == null) {
        log.info("Secret not found");
        throw new ConfigDataResourceNotFoundException(resource);
      }

      log.debug(String.format("now gain %d secretMap in secret, storename = %s, secretname = %s",
          secretMap.size(), resource.getStoreName(), resource.getSecretName()));

      List<PropertySource<?>> sourceList = new ArrayList<>(
          DaprCloudConfigParserHandler.getInstance().parseDaprCloudConfigData(
              resource.getStoreName() + ":" + resource.getSecretName(),
              secretMap,
              resource.getType()
          ));

      log.debug(String.format("now gain %d data source in secret, storename = %s, secretname = %s",
          sourceList.size(), resource.getStoreName(), resource.getSecretName()));
      return new ConfigData(sourceList, IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
    } catch (RuntimeException e) {
      log.info("Failed to get secret from sidecar: " + e.getMessage(), e);
      throw new IOException("Failed to get secret from sidecar", e);
    }
  }

  protected <T> T getBean(ConfigDataLoaderContext context, Class<T> type) {
    if (context.getBootstrapContext().isRegistered(type)) {
      return context.getBootstrapContext().get(type);
    }
    return null;
  }
}
