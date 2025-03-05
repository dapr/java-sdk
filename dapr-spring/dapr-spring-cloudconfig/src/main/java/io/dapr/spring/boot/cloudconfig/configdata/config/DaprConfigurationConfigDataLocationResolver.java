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

import io.dapr.spring.boot.autoconfigure.client.DaprClientProperties;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigClientManager;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigProperties;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.*;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

public class DaprConfigurationConfigDataLocationResolver
        implements ConfigDataLocationResolver<DaprConfigurationConfigDataResource>, Ordered {

    public static final String PREFIX = "dapr:config:";

    private final Log log;

    public DaprConfigurationConfigDataLocationResolver(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    /**
     * Returns if the specified location address contains dapr prefix.
     *
     * @param context  the location resolver context
     * @param location the location to check.
     * @return if the location is supported by this resolver
     */
    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        log.debug(String.format("checking if %s suits for dapr config", location.toString()));
        return location.hasPrefix(PREFIX);
    }

    /**
     * Resolve a {@link ConfigDataLocation} into one or more {@link ConfigDataResource} instances.
     *
     * @param context  the location resolver context
     * @param location the location that should be resolved
     * @return a list of {@link ConfigDataResource resources} in ascending priority order.
     * @throws ConfigDataLocationNotFoundException on a non-optional location that cannot be found
     * @throws ConfigDataResourceNotFoundException if a resolved resource cannot be found
     */
    @Override
    public List<DaprConfigurationConfigDataResource> resolve(ConfigDataLocationResolverContext context,
                                                             ConfigDataLocation location)
            throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {

        DaprCloudConfigProperties daprSecretStoreConfig = loadProperties(context);
        DaprClientProperties daprClientConfig = loadClientProperties(context);

        ConfigurableBootstrapContext bootstrapContext = context
                .getBootstrapContext();

        registerConfigManager(daprSecretStoreConfig, daprClientConfig, bootstrapContext);

        List<DaprConfigurationConfigDataResource> result = new ArrayList<>();

        String[] secretConfig = location.getNonPrefixedValue(PREFIX).split("/");

        log.info(String.format("there is %d of values in secretConfig", secretConfig.length));

        switch (secretConfig.length) {
            case 2:
                log.debug("Dapr Secret Store now gains store name: '" + secretConfig[0] + "' and secret name: '"
                        + secretConfig[1] + "' secret store for config");
                result.add(
                        new DaprConfigurationConfigDataResource(location.isOptional(), secretConfig[0], secretConfig[1]));
                break;
            case 1:
                log.debug("Dapr Secret Store now gains store name: '" + secretConfig[0] + "' secret store for config");
                result.add(new DaprConfigurationConfigDataResource(location.isOptional(), secretConfig[0], null));
                break;
            default:
                throw new ConfigDataLocationNotFoundException(location);
        }

        return result;
    }

    /**
     * @return
     */
    @Override
    public int getOrder() {
        return -1;
    }

    private void registerConfigManager(DaprCloudConfigProperties properties,
                                       DaprClientProperties clientConfig,
                                       ConfigurableBootstrapContext bootstrapContext) {
        if (!bootstrapContext.isRegistered(DaprCloudConfigClientManager.class)) {
            bootstrapContext.register(DaprCloudConfigClientManager.class,
                    BootstrapRegistry.InstanceSupplier
                            .of(new DaprCloudConfigClientManager(properties, clientConfig)));
        }
    }

    protected DaprCloudConfigProperties loadProperties(
            ConfigDataLocationResolverContext context) {
        Binder binder = context.getBinder();
        BindHandler bindHandler = getBindHandler(context);

        DaprCloudConfigProperties daprCloudConfigProperties;
        if (context.getBootstrapContext().isRegistered(DaprCloudConfigProperties.class)) {
            daprCloudConfigProperties = context.getBootstrapContext()
                    .get(DaprCloudConfigProperties.class);
        } else {
            daprCloudConfigProperties = binder
                    .bind(DaprCloudConfigProperties.PROPERTY_PREFIX, Bindable.of(DaprCloudConfigProperties.class),
                            bindHandler)
                    .orElseGet(DaprCloudConfigProperties::new);
        }

        return daprCloudConfigProperties;
    }

    protected DaprClientProperties loadClientProperties(
            ConfigDataLocationResolverContext context) {
        Binder binder = context.getBinder();
        BindHandler bindHandler = getBindHandler(context);

        DaprClientProperties daprClientConfig;
        if (context.getBootstrapContext().isRegistered(DaprClientProperties.class)) {
            daprClientConfig = context.getBootstrapContext()
                    .get(DaprClientProperties.class);
        } else {
            daprClientConfig = binder
                    .bind(DaprClientProperties.PROPERTY_PREFIX, Bindable.of(DaprClientProperties.class),
                            bindHandler)
                    .orElseGet(DaprClientProperties::new);
        }

        return daprClientConfig;
    }

    private BindHandler getBindHandler(ConfigDataLocationResolverContext context) {
        return context.getBootstrapContext().getOrElse(BindHandler.class, null);
    }
}
