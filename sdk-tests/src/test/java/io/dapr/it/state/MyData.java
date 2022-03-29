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
