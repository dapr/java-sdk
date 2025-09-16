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

package io.dapr.spring.boot.cloudconfig.autoconfigure;

import io.dapr.client.DaprClient;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DaprCloudConfigProperties.class)
@ConditionalOnProperty(name = DaprCloudConfigProperties.PROPERTY_PREFIX + ".enabled", matchIfMissing = true)
@ConditionalOnClass(DaprClient.class)
public class DaprCloudConfigAutoConfiguration {

}
