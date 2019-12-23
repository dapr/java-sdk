/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Implement distributed tracing.
// TODO: Make this generic to thw SDK and not only for Actors.
public final class ActorTrace {

  private static final Logger LOGGER = Logger.getLogger(ActorTrace.class.getName());

  public void writeInfo(String type, String id, String msgFormat, Object... params) {
    this.write(Level.INFO, type, id, msgFormat, params);
  }

  public void writeWarning(String type, String id, String msgFormat, Object... params) {
    this.write(Level.WARNING, type, id, msgFormat, params);
  }

  public void writeError(String type, String id, String msgFormat, Object... params) {
    this.write(Level.SEVERE, type, id, msgFormat, params);
  }

  private void write(Level level, String type, String id, String msgFormat, Object... params) {
    String formatString = String.format("%s:%s %s", emptyIfNul(type), emptyIfNul(id), emptyIfNul(msgFormat));
    if ((params == null) || (params.length == 0)) {
      LOGGER.log(level, formatString);
    } else {
      LOGGER.log(level, String.format(formatString, params));
    }
  }

  private static String emptyIfNul(String s) {
    if (s == null) {
      return "";
    }

    return s;
  }
}
