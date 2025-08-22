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

import io.dapr.spring.boot.autoconfigure.client.DaprClientProperties;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigClientManager;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigProperties;
import io.dapr.spring.boot.cloudconfig.configdata.types.DaprCloudConfigType;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

public class DaprSecretStoreConfigDataLocationResolver
    implements ConfigDataLocationResolver<DaprSecretStoreConfigDataResource>, Ordered {

  public static final String PREFIX = "dapr:secret:";

  private final Log log;

  public DaprSecretStoreConfigDataLocationResolver(DeferredLogFactory logFactory) {
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
    log.debug(String.format("checking if %s suits for dapr secret", location.toString()));
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
  public List<DaprSecretStoreConfigDataResource> resolve(ConfigDataLocationResolverContext context,
                                                         ConfigDataLocation location)
      throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {

    DaprCloudConfigProperties daprSecretStoreConfig = loadProperties(context);
    DaprClientProperties daprClientConfig = loadClientProperties(context);

    ConfigurableBootstrapContext bootstrapContext = context
        .getBootstrapContext();

    registerConfigManager(daprSecretStoreConfig, daprClientConfig, bootstrapContext);

    List<DaprSecretStoreConfigDataResource> result = new ArrayList<>();

    // To avoid UriComponentsBuilder to decode a wrong host.
    String fullConfig = "secret://" + location.getNonPrefixedValue(PREFIX);

    UriComponents configUri = UriComponentsBuilder.fromUriString(fullConfig).build();

    String storeName = configUri.getHost();
    String secretPath = configUri.getPath();
    String secretName = StringUtils.hasText(secretPath)
        ? StringUtils.trimLeadingCharacter(secretPath, '/')
        : null;


    MultiValueMap<String, String> typeQuery = configUri.getQueryParams();
    DaprCloudConfigType secretType = DaprCloudConfigType.fromString(typeQuery.getFirst("type"),
        typeQuery.getFirst("doc-type"));

    if (secretName == null) {
      log.debug("Dapr Secret Store now gains store name: '" + storeName + "' secret store for config");
      result.add(new DaprSecretStoreConfigDataResource(location.isOptional(), storeName, null, secretType));
    } else if (secretName.contains("/")) {
      throw new ConfigDataLocationNotFoundException(location);
    } else {
      log.debug("Dapr Secret Store now gains store name: '" + storeName + "' and secret name: '"
          + secretName + "' secret store for config");
      result.add(
          new DaprSecretStoreConfigDataResource(location.isOptional(), storeName, secretName, secretType));
    }

    return result;
  }

  @Override
  public int getOrder() {
    return -1;
  }

  private void registerConfigManager(DaprCloudConfigProperties properties,
                                     DaprClientProperties clientConfig,
                                     ConfigurableBootstrapContext bootstrapContext) {
    synchronized (DaprCloudConfigClientManager.class) {
      if (!bootstrapContext.isRegistered(DaprCloudConfigClientManager.class)) {
        bootstrapContext.register(DaprCloudConfigClientManager.class,
            BootstrapRegistry.InstanceSupplier
                .of(new DaprCloudConfigClientManager(properties, clientConfig)));
      }
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
