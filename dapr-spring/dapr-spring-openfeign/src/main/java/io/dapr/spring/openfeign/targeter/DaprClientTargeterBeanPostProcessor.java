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


package io.dapr.spring.openfeign.targeter;

import io.dapr.feign.DaprInvokeFeignClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.openfeign.Targeter;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(Targeter.class)
public class DaprClientTargeterBeanPostProcessor implements BeanPostProcessor {

  private final DaprInvokeFeignClient daprInvokeFeignClient;

  public DaprClientTargeterBeanPostProcessor(DaprInvokeFeignClient daprInvokeFeignClient) {
    this.daprInvokeFeignClient = daprInvokeFeignClient;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof Targeter) {
      return new DaprClientTargeter(daprInvokeFeignClient, (Targeter) bean);
    }
    return bean;
  }
}
