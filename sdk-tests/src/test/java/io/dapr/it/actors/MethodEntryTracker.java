/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import java.util.Date;

public class MethodEntryTracker {
  private boolean isEnter;
  private String methodName;
  private Date date;

  public MethodEntryTracker(boolean isEnter, String methodName, Date date) {
    this.isEnter = isEnter;
    this.methodName = methodName;
    this.date = date;
  }

  public boolean getIsEnter() {
    return this.isEnter;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public Date getDate() {
    return this.date;
  }

  @Override
  public String toString() {
    return this.date + " " + this.isEnter + " " + this.methodName;
  }
}
