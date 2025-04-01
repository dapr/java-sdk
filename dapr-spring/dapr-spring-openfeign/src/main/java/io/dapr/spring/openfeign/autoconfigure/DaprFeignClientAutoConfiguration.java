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

package io.dapr.spring.openfeign.autoconfigure;

import io.dapr.client.DaprClient;
import io.dapr.feign.DaprInvokeFeignClient;
import io.dapr.spring.openfeign.targeter.DaprClientTargeter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.Targeter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DaprFeignClientProperties.class)
@ConditionalOnProperty(name = "dapr.feign.enabled", matchIfMissing = true)
@ConditionalOnClass(FeignAutoConfiguration.class)
public class DaprFeignClientAutoConfiguration {

  @Bean
  public Targeter targeter(DaprInvokeFeignClient daprInvokeFeignClient) {
    return new DaprClientTargeter(daprInvokeFeignClient);
  }

  @Bean
  @ConditionalOnMissingBean
  public DaprInvokeFeignClient daprInvokeFeignClient(DaprClient daprClient) {
    return new DaprInvokeFeignClient(daprClient);
  }
}
