/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.state;

import java.util.Objects;

public class MyData {

  /// Gets or sets the value for PropertyA.
  private String propertyA;

  /// Gets or sets the value for PropertyB.
  private String propertyB;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MyData myData = (MyData) o;
    return Objects.equals(propertyA, myData.propertyA) &&
        Objects.equals(propertyB, myData.propertyB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyA, propertyB);
  }
}
