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
 
  public String newValue;
  private int newInt;
  public double newDouble;

  public CloudEventCustom() {
    super();
  }

  public CloudEventCustom(
      String id,
      String source,
      String type,
      String specversion,
      String datacontenttype,
      T data,
      String newValue,
      int newInt,
      double newDouble) {
      super(id, source, type, specversion, datacontenttype, data);
      this.newValue = newValue;
      this.newInt = newInt;
      this.newDouble = newDouble;
  }

  public CloudEventCustom(
      String id,
      String source,
      String type,
      String specversion,
      byte[] binaryData,
      String newValue,
      int newInt,
      double newDouble
      ) {
      super(id, source, type, specversion, binaryData);
      this.newValue = newValue;
      this.newInt = newInt;
      this.newDouble = newDouble;

  } 

  public int getNewInt() {
    return newInt;
  }

}