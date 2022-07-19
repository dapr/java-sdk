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

package io.dapr.client.domain;

/**
 * Domain object for unsubscribe response.
 */
public class UnsubscribeConfigurationResponse {
  /**
   * Boolean denoting whether unsubscribe API call is success/failure.
   */
  private final boolean isUnsubscribed;
  /**
   * Unsubscribe API response message.
   */
  private final String message;

  /**
   * Constructor for UnsubscribeConfigurationResponse.
   *
   * @param isUnsubscribed boolean denoting unsubscribe API response.
   * @param message unsubscribe API response message.
   */
  public UnsubscribeConfigurationResponse(boolean isUnsubscribed, String message) {
    this.isUnsubscribed = isUnsubscribed;
    this.message = message;
  }

  public boolean getIsUnsubscribed() {
    return isUnsubscribed;
  }

  public String getMessage() {
    return message;
  }
}
