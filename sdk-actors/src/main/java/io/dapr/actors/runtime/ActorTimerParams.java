/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

/**
 * Parameters for Actor Timer.
 */
final class ActorTimerParams {

  /**
   * Callback function to be invoked in actor.
   */
  private String callback;

  /**
   * Data to be passed in as part of the timer trigger.
   */
  private byte[] data;

  /**
   * Sets the name of the callback function.
   *
   * @param callback Name of the callback function.
   */
  public void setCallback(String callback) {
    this.callback = callback;
  }

  /**
   * Gets the name of the callback function.
   *
   * @return Name of the callback function.
   */
  public String getCallback() {
    return this.callback;
  }

  /**
   * Sets the raw data for the callback function.
   *
   * @param data Raw data for the callback function.
   */
  public void setData(byte[] data) {
    this.data = data;
  }

  /**
   * Gets the raw data for the callback function.
   *
   * @return Raw data for the callback function.
   */
  public byte[] getData() {
    return data;
  }

}
