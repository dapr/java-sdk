/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.client;

import io.dapr.client.domain.SubscribeConfigurationResponse;
import java.util.logging.Logger;

/**
 * Base class to handle configuration subscribe response.
 * Application needs to extend this calls to have better control on response handelling
 */
public class BaseSubscribeConfigHandler {
  private static final Logger logger = Logger.getLogger(BaseSubscribeConfigHandler.class.getName());

  /**
   * Store the reference for child class implementation.
   */
  private BaseSubscribeConfigHandler configSubscriberHandlerImpl = null;

  /**
   * Singleton instance of this class.
   */
  private static volatile BaseSubscribeConfigHandler instance;

  /**
   * Default constructor.
   */
  public BaseSubscribeConfigHandler() {
  }

  /**
   * Method to create a singelton instance.
   * @return {@link BaseSubscribeConfigHandler}
   */
  public static BaseSubscribeConfigHandler getInstance() {
    if (instance == null) {
      synchronized (BaseSubscribeConfigHandler.class) {
        if (instance == null) {
          instance = new BaseSubscribeConfigHandler();
        }
      }
    }

    return instance;
  }

  /**
   * Setter method for configSubscriberHandlerImpl.
   * @param configSubscriberHandlerImpl {@link BaseSubscribeConfigHandler}
   */
  public void setConfigSubscriberHandlerImpl(BaseSubscribeConfigHandler configSubscriberHandlerImpl) {
    this.configSubscriberHandlerImpl = configSubscriberHandlerImpl;
  }

  /**
   * Getter method for configSubscriberHandlerImpl.
   * @return instance of type {@link BaseSubscribeConfigHandler}
   */
  public BaseSubscribeConfigHandler getConfigSubscriberHandlerImpl() {
    return this.configSubscriberHandlerImpl;
  }

  /**
   * Method to be called by spring boot controller to extend the reponse handelling.
   * @param response {@link SubscribeConfigurationResponse}
   */
  public void handleResponse(SubscribeConfigurationResponse response) {
    if (this.configSubscriberHandlerImpl == null) {
      logger.warning("You need to extend this class to have better control on response");
    } else {
      this.configSubscriberHandlerImpl.handleResponse(response);
    }
  }

}
