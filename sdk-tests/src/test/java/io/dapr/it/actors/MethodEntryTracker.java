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
