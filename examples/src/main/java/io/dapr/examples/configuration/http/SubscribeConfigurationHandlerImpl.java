/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.examples.configuration.http;

import io.dapr.client.BaseSubscribeConfigHandler;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.SubscribeConfigurationResponse;


import java.util.Map;

/**
 * Child class to handle response from {@link io.dapr.springboot.DaprController}.
 */
public class SubscribeConfigurationHandlerImpl extends BaseSubscribeConfigHandler {

  /**
   * Method to handle the reponse from SubscribeConfiguration.
   * @param response {@link SubscribeConfigurationResponse}
   */
  public void handleResponse(SubscribeConfigurationResponse response) {
    Map<String, ConfigurationItem> items = response.getItems();
    for (Map.Entry<String, ConfigurationItem> entry : items.entrySet()) {
      System.out.println(entry.getValue().getValue() + " : sp key ->" + entry.getKey());
    }
  }
}
