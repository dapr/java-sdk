/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.client;

import io.dapr.client.domain.CloudEvent;

import java.util.Arrays;
import java.util.Objects;

import java.io.IOException;


public class CloudEventCustom<T> extends CloudEvent<T> {
 
  private String newValue;

  private int newInt;

  private double newDouble;

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public int getNewInt() {
    return newInt;
  }

  public void setNewInt(int newInt) {
    this.newInt = newInt;
  }

  public double getNewDouble() {
    return newDouble;
  }

  public void setNewDouble(double newDouble) {
    this.newDouble = newDouble;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    CloudEventCustom<?> that = (CloudEventCustom<?>) o;
    return newInt == that.newInt && Double.compare(that.newDouble, newDouble) == 0 && Objects.equals(newValue, that.newValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), newValue, newInt, newDouble);
  }

}