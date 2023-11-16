/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.workflows.saga;

/**
 * Context for a compensation activity.
 */
public class CompensatationContext {
  private String activityClassName;
  private Object activityInput;
  private Object activityOutput;

  /**
   * Default Constructor for a compensation activity.
   * 
   */
  public CompensatationContext() {
  }

  /**
   * Constructor for a compensation activity.
   * 
   * @param activityClassName Class name of the activity.
   * @param activityInput     Input of the activity.
   * @param activityOutput    Output of the activity.
   */
  public CompensatationContext(String activityClassName, Object activityInput,
      Object activityOutput) {
    this.activityClassName = activityClassName;
    this.activityInput = activityInput;
    this.activityOutput = activityOutput;
  }

  /**
   * Gets the class name of the activity.
   * 
   * @return the class name of the activity.
   */
  public String getActivityClassName() {
    return activityClassName;
  }

  /**
   * Gets the input of the activity.
   * 
   * @return the input of the activity.
   */
  public Object getActivityInput() {
    return activityInput;
  }

  /**
   * Gets the output of the activity.
   * 
   * @return the output of the activity.
   */
  public Object getActivityOutput() {
    return activityOutput;
  }

  /**
   * set the class name of the activity.
   * 
   * @param activityClassName the class name of the activity.
   */
  public void setActivityClassName(String activityClassName) {
    this.activityClassName = activityClassName;
  }

  /**
   * set the input of the activity.
   * 
   * @param activityInput the input of the activity.
   */
  public void setActivityInput(Object activityInput) {
    this.activityInput = activityInput;
  }

  /**
   * set the output of the activity.
   * 
   * @param activityOutput the output of the activity.
   */
  public void setActivityOutput(Object activityOutput) {
    this.activityOutput = activityOutput;
  }

}