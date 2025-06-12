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

package io.dapr.client.domain;

public enum UnlockResponseStatus {

  /**
   * The unlock operation succeeded.
   */
  SUCCESS(0),

  /**
   * The target you want to unlock does not exist.
   */
  LOCK_UNEXIST(1),

  /**
   * The desired target does not match.
   */
  LOCK_BELONG_TO_OTHERS(2),

  /**
   * Internal error.
   */
  INTERNAL_ERROR(3);
  private final Integer code;

  UnlockResponseStatus(Integer code) {
    this.code = code;
  }

  public Integer getCode() {
    return code;
  }

  /**
   * Convert the status code to a UnlockResponseStatus object.
   * @param code status code
   * @return UnlockResponseStatus
   */
  public static UnlockResponseStatus valueOf(int code) {
    for (UnlockResponseStatus status : UnlockResponseStatus.values()) {
      if (status.getCode().equals(code)) {
        return status;
      }
    }
    return INTERNAL_ERROR;
  }
}
