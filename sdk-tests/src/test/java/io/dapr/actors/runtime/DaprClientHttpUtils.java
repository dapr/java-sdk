/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import io.dapr.client.DaprHttp;

/**
 * Exposes useful methods for IT in DaprClientHttp.
 */
public class DaprClientHttpUtils {

  public static void unregisterActorReminder(
      DaprHttp client,
      String actorType,
      String actorId,
      String reminderName) throws Exception {
    new DaprHttpClient(client).unregisterActorReminder(actorType, actorId, reminderName).block();
  }
}
