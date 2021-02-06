/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Implement distributed tracing.
// TODO: Make this generic to the SDK and not only for Actors.

/**
 * Class to emit trace log messages.
 */
public final class ActorTrace {

  /**
   * Gets the default Logger.
   */
  private static final Logger LOGGER = Logger.getLogger(ActorTrace.class.getName());

  /**
   * Writes an information trace log.
   *
   * @param type      Type of log.
   * @param id        Instance identifier.
   * @param msgFormat Message or message format (with type and id input as well).
   * @param params    Params for the message.
   */
  public void writeInfo(String type, String id, String msgFormat, Object... params) {
    this.write(Level.INFO, type, id, msgFormat, params);
  }

  /**
   * Writes an warning trace log.
   *
   * @param type      Type of log.
   * @param id        Instance identifier.
   * @param msgFormat Message or message format (with type and id input as well).
   * @param params    Params for the message.
   */
  public void writeWarning(String type, String id, String msgFormat, Object... params) {
    this.write(Level.WARNING, type, id, msgFormat, params);
  }

  /**
   * Writes an error trace log.
   *
   * @param type      Type of log.
   * @param id        Instance identifier.
   * @param msgFormat Message or message format (with type and id input as well).
   * @param params    Params for the message.
   */
  public void writeError(String type, String id, String msgFormat, Object... params) {
    this.write(Level.SEVERE, type, id, msgFormat, params);
  }

  /**
   * Writes a trace log.
   *
   * @param level     Severity level of the log.
   * @param type      Type of log.
   * @param id        Instance identifier.
   * @param msgFormat Message or message format (with type and id input as well).
   * @param params    Params for the message.
   */
  private void write(Level level, String type, String id, String msgFormat, Object... params) {
    String formatString = String.format("%s:%s %s", emptyIfNul(type), emptyIfNul(id), emptyIfNul(msgFormat));
    if ((params == null) || (params.length == 0)) {
      LOGGER.log(level, formatString);
    } else {
      LOGGER.log(level, String.format(formatString, params));
    }
  }

  /**
   * Utility method that returns empty if String is null.
   *
   * @param s String to be checked.
   * @return String (if not null) or empty (if null).
   */
  private static String emptyIfNul(String s) {
    if (s == null) {
      return "";
    }

    return s;
  }
}
