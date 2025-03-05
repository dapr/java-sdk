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
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigClientManager;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigProperties;
import io.dapr.spring.boot.cloudconfig.parser.DaprSecretStoreParserHandler;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.context.config.ConfigData.Option.*;

public class DaprConfigurationConfigDataLoader implements ConfigDataLoader<DaprConfigurationConfigDataResource> {

    private final Log log;

    private DaprClient daprClient;

    private DaprCloudConfigProperties daprSecretStoreConfig;

    public DaprConfigurationConfigDataLoader(DeferredLogFactory logFactory, DaprClient daprClient,
                                             DaprCloudConfigProperties daprSecretStoreConfig) {
        this.log = logFactory.getLog(getClass());
        this.daprClient = daprClient;
        this.daprSecretStoreConfig = daprSecretStoreConfig;
    }


    /**
     * Load {@link ConfigData} for the given resource.
     *
     * @param context the loader context
     * @param resource the resource to load
     * @return the loaded config data or {@code null} if the location should be skipped
     * @throws IOException on IO error
     * @throws ConfigDataResourceNotFoundException if the resource cannot be found
     */
    @Override
    public ConfigData load(ConfigDataLoaderContext context, DaprConfigurationConfigDataResource resource)
            throws IOException, ConfigDataResourceNotFoundException {
        DaprCloudConfigClientManager daprClientSecretStoreConfigManager =
                getBean(context, DaprCloudConfigClientManager.class);

        daprClient = DaprCloudConfigClientManager.getDaprClient();
        daprSecretStoreConfig = daprClientSecretStoreConfigManager.getDaprCloudConfigProperties();

        if (resource.getSecretName() == null) {
            return fetchConfig(resource.getStoreName());
        } else {
            return fetchConfig(resource.getStoreName(), resource.getSecretName());
        }
    }

    private ConfigData fetchConfig(String storeName) {
        Mono<Map<String, Map<String, String>>> secretMapMono = daprClient.getBulkSecret(storeName);

        Map<String, Map<String, String>> secretMap =
                secretMapMono.block(Duration.ofMillis(daprSecretStoreConfig.getTimeout()));

        if (secretMap == null) {
            return new ConfigData(Collections.emptyList(), IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
        }

        List<PropertySource<?>> sourceList = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : secretMap.entrySet()) {
            sourceList.addAll(DaprSecretStoreParserHandler.getInstance().parseDaprSecretStoreData(entry.getKey(),
                    entry.getValue()));
        }

        return new ConfigData(sourceList, IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
    }

    private ConfigData fetchConfig(String storeName, String secretName) {
        Mono<Map<String, String>> secretMapMono = daprClient.getSecret(storeName, secretName);

        Map<String, String> secretMap = secretMapMono.block(Duration.ofMillis(daprSecretStoreConfig.getTimeout()));

        if (secretMap == null) {
            return new ConfigData(Collections.emptyList(), IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
        }

        List<PropertySource<?>> sourceList = new ArrayList<>(
                DaprSecretStoreParserHandler.getInstance().parseDaprSecretStoreData(secretName, secretMap));

        return new ConfigData(sourceList, IGNORE_IMPORTS, IGNORE_PROFILES, PROFILE_SPECIFIC);
    }

    protected <T> T getBean(ConfigDataLoaderContext context, Class<T> type) {
        if (context.getBootstrapContext().isRegistered(type)) {
            return context.getBootstrapContext().get(type);
        }
        return null;
    }
}
