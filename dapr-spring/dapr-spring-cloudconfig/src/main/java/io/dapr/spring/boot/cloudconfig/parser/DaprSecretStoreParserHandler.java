/*
 * Copyright (c) 2016-2024 Team Fangkehou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.spring.boot.cloudconfig.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;

public class DaprSecretStoreParserHandler {

    private static List<PropertySourceLoader> propertySourceLoaders;

    private DaprSecretStoreParserHandler() {
        propertySourceLoaders = SpringFactoriesLoader
                .loadFactories(PropertySourceLoader.class, getClass().getClassLoader());
    }

    public List<PropertySource<?>> parseDaprSecretStoreData(String configName, Map<String, String> configValue) {
        List<PropertySource<?>> result = new ArrayList<>();

        List<String> configList = new ArrayList<>();
        configValue.forEach((key, value) -> configList.add(String.format("%s=%s", key, value)));

        Resource configResult = new ByteArrayResource(String.join("\n", configList).getBytes());

        for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
            try {
                result.addAll(propertySourceLoader.load(configName, configResult));
            } catch (IOException ignored) {
            }
        }

        return result;
    }

    public static DaprSecretStoreParserHandler getInstance() {
        return ParserHandler.HANDLER;
    }

    private static class ParserHandler {

        private static final DaprSecretStoreParserHandler HANDLER = new DaprSecretStoreParserHandler();

    }
}
