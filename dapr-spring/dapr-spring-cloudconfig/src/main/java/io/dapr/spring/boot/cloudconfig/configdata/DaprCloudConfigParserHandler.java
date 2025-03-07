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

package io.dapr.spring.boot.cloudconfig.configdata;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaprCloudConfigParserHandler {

  private static List<PropertySourceLoader> propertySourceLoaders;

  private DaprCloudConfigParserHandler() {
    propertySourceLoaders = SpringFactoriesLoader
        .loadFactories(PropertySourceLoader.class, getClass().getClassLoader());
  }

  public static DaprCloudConfigParserHandler getInstance() {
    return ParserHandler.HANDLER;
  }

  /**
   * Parse Secret using PropertySourceLoaders.
   *
   * <p>
   *   if type = doc, will treat all values as a property source (both "properties" or "yaml" format supported)
   * </p>
   *
   * <p>
   *   if type = value, will transform key and value to "key=value" format ("properties" format)
   * </p>
   *
   * @param configName name of the config
   * @param configValue value of the config
   * @param type value type
   * @return property source list
   */
  public List<PropertySource<?>> parseDaprSecretStoreData(
      String configName,
      Map<String, String> configValue,
      DaprCloudConfigType type
  ) {
    List<PropertySource<?>> result = new ArrayList<>();

    Map<String, Resource> configResults = getConfigResult(configValue, type);

    configResults.forEach((key, configResult) -> {
      for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
        String fullConfigName = StringUtils.hasText(key) ? configName + "." + key : configName;
        try {
          result.addAll(propertySourceLoader.load(fullConfigName, configResult));
        } catch (IOException ignored) {
          continue;
        }
      }
    });

    return result;
  }

  private Map<String, Resource> getConfigResult(
      Map<String, String> configValue,
      DaprCloudConfigType type
  ) {
    Map<String, Resource> result = new HashMap<>();
    if (DaprCloudConfigType.Doc.equals(type)) {
      configValue.forEach((key, value) -> result.put(key,
          new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8))));
    } else {
      List<String> configList = new ArrayList<>();
      configValue.forEach((key, value) -> configList.add(String.format("%s=%s", key, value)));
      result.put("", new ByteArrayResource(String.join("\n", configList).getBytes(StandardCharsets.UTF_8)));
    }
    return result;
  }

  private static class ParserHandler {

    private static final DaprCloudConfigParserHandler HANDLER = new DaprCloudConfigParserHandler();

  }
}
