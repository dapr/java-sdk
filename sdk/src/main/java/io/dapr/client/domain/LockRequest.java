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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * A request to lock.
 */
public class LockRequest implements Serializable {

  /**
   * The lock store name,e.g. `redis`.
   */
  @JsonIgnore
  private final String storeName;

  /**
   * Required. resourceId is the lock key. e.g. `order_id_111`
   * It stands for "which resource I want to protect"
   */
  private final String resourceId;

  /**
   * lockOwner indicate the identifier of lock owner.
   * You can generate a uuid as lock_owner.For example,in golang:
   * req.LockOwner = uuid.New().String()
   * This field is per request,not per process,so it is different for each request,
   * which aims to prevent multi-thread in the same process trying the same lock concurrently.
   * The reason why we don't make it automatically generated is:
   * 1. If it is automatically generated,there must be a 'my_lock_owner_id' field in the response.
   * This name is so weird that we think it is inappropriate to put it into the api spec
   * 2. If we change the field 'my_lock_owner_id' in the response to 'lock_owner',
   * which means the current lock owner of this lock,
   * we find that in some lock services users can't get the current lock owner.
   * Actually users don't need it at all.
   * 3. When reentrant lock is needed,the existing lock_owner is required to identify client
   * and check "whether this client can reenter this lock".
   * So this field in the request shouldn't be removed.
   */
  private final String lockOwner;

  /**
   * The time before expiry.The time unit is second.
   */
  private final Integer expiryInSeconds;

  /**
   * Constructor of LockRequest.
   * @param storeName Name of the store
   * @param resourceId Lock key
   * @param lockOwner The identifier of lock owner
   * @param expiryInSeconds The time before expiry
   */
  public LockRequest(String storeName, String resourceId, String lockOwner, Integer expiryInSeconds) {
    this.storeName = storeName;
    this.resourceId = resourceId;
    this.lockOwner = lockOwner;
    this.expiryInSeconds = expiryInSeconds;
  }

  public String getStoreName() {
    return storeName;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getLockOwner() {
    return lockOwner;
  }

  public Integer getExpiryInSeconds() {
    return expiryInSeconds;
  }
}
