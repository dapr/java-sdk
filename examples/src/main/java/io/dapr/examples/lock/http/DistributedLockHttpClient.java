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

package io.dapr.examples.lock.http;


import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.LockRequest;
import io.dapr.client.domain.UnlockRequest;
import io.dapr.client.domain.UnlockResponseStatus;
import reactor.core.publisher.Mono;

/**
 * DistributedLockGrpcClient.
 */
public class DistributedLockHttpClient {
  private static final String LOCK_STORE_NAME = "lockstore";

  /**
   * Executes various methods to check the different apis.
   *
   * @param args arguments
   * @throws Exception throws Exception
   */
  public static void main(String[] args) throws Exception {
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      tryLock(client);
      unlock(client);
    }
  }

  /**
   * Trying to get lock.
   *
   * @param client DaprPreviewClient object
   */
  public static void tryLock(DaprPreviewClient client) {
    System.out.println("*******trying to get a free distributed lock********");
    try {
      LockRequest lockRequest = new LockRequest(LOCK_STORE_NAME, "resouce1", "owner1", 5);
      Mono<Boolean> result = client.tryLock(lockRequest);
      System.out.println("Lock result -> " + (Boolean.TRUE.equals(result.block()) ? "SUCCESS" : "FAIL"));
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

  /**
   * Unlock a lock.
   *
   * @param client DaprPreviewClient object
   */
  public static void unlock(DaprPreviewClient client) {
    System.out.println("*******unlock a distributed lock********");
    try {
      UnlockRequest unlockRequest = new UnlockRequest(LOCK_STORE_NAME, "resouce1", "owner1");
      Mono<UnlockResponseStatus> result = client.unlock(unlockRequest);
      System.out.println("Unlock result ->" + result.block().name());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
