/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

public class MyData {

  /// Gets or sets the value for PropertyA.
  private String propertyA;

  /// Gets or sets the value for PropertyB.
  private String propertyB;

  private MyData myData;

  public String getPropertyB() {
    return propertyB;
  }

  public void setPropertyB(String propertyB) {
    this.propertyB = propertyB;
  }

  public String getPropertyA() {
    return propertyA;
  }

  public void setPropertyA(String propertyA) {
    this.propertyA = propertyA;
  }

  @Override
  public String toString() {
    return "MyData{" +
      "propertyA='" + propertyA + '\'' +
      ", propertyB='" + propertyB + '\'' +
      '}';
  }

  public MyData getMyData() {
    return myData;
  }

  public void setMyData(MyData myData) {
    this.myData = myData;
  }
}
