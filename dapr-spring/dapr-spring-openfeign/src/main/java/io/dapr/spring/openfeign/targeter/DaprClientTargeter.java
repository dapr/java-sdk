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

import feign.Client;
import feign.Feign;
import feign.Target;
import io.dapr.feign.DaprInvokeFeignClient;
import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.Targeter;

import java.lang.reflect.Field;

public class DaprClientTargeter implements Targeter {

  private final DaprInvokeFeignClient daprInvokeFeignClient;
  private final Targeter targeter;

  public DaprClientTargeter(DaprInvokeFeignClient daprInvokeFeignClient, Targeter targeter) {
    this.daprInvokeFeignClient = daprInvokeFeignClient;
    this.targeter = targeter;
  }

  @Override
  public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignClientFactory context,
                      Target.HardCodedTarget<T> target) {
    Class<T> classOfT = target.type();
    UseDaprClient useDaprClient = classOfT.getAnnotation(UseDaprClient.class);

    if (useDaprClient == null) {
      return targeter.target(
          factory,
          feign,
          context,
          target
      );
    }

    Class<Feign.Builder> builderClass = Feign.Builder.class;

    Client defaultClient = null;

    try {
      Field clientField = builderClass.getDeclaredField("client");
      clientField.setAccessible(true);

      defaultClient = (Client) clientField.get(feign);

    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    feign.client(daprInvokeFeignClient);

    T targetInstance = feign.target(target);

    feign.client(defaultClient);

    return targetInstance;
  }
}
