/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.exceptions.DaprException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test for the HTTP Async Client.
 * <p>
 * Requires Dapr running.
 */
public class DaprHttpAsyncClientIT {

  /**
   * Checks if the error is correctly parsed when trying to invoke a function on
   * an unknown actor type.
   */
  @Test(expected = RuntimeException.class)
  public void invokeUnknownActor() {
    ActorProxyAsyncClient daprAsyncClient = new ActorProxyClientBuilder().buildAsyncClient();
    daprAsyncClient
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
