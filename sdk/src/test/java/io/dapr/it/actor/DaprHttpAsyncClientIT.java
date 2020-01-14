/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.it.actor;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.exceptions.DaprException;
import io.dapr.it.BaseIT;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test for the HTTP Async Client.
 * <p>
 * Requires Dapr running.
 */
public class DaprHttpAsyncClientIT extends BaseIT {

  /**
   * Checks if the error is correctly parsed when trying to invoke a function on
   * an unknown actor type.
   */
  @Test(expected = RuntimeException.class)
  public void invokeUnknownActor() {
    DaprClient daprClient = new DaprClientBuilder().build();
    daprClient
      .invokeActorMethod("ActorThatDoesNotExist", "100", "GetData", null)
      .doOnError(x -> {
        Assert.assertTrue(x instanceof RuntimeException);
        RuntimeException runtimeException = (RuntimeException) x;

        Throwable cause = runtimeException.getCause();
        Assert.assertTrue(cause instanceof DaprException);
        DaprException daprException = (DaprException) cause;

        Assert.assertNotNull(daprException);
        Assert.assertEquals("ERR_INVOKE_ACTOR", daprException.getErrorCode());
        Assert.assertNotNull(daprException.getMessage());
        Assert.assertFalse(daprException.getMessage().isEmpty());
      })
      .doOnSuccess(x -> Assert.fail("This call should fail."))
      .block();
  }
}
