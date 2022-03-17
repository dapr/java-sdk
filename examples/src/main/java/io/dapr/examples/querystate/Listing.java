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

package io.dapr.examples.querystate;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Listing {

  @JsonProperty
  private String propertyType;

  @JsonProperty
  private String id;

  @JsonProperty
  private String city;

  @JsonProperty
  private String state;

  public Listing() {
  }

  public String getPropertyType() {
    return propertyType;
  }

  public void setPropertyType(String propertyType) {
    this.propertyType = propertyType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return "Listing{"
        + "propertyType='" + propertyType + '\''
        + ", id=" + id
        + ", city='" + city + '\''
        + ", state='" + state + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Listing listing = (Listing) o;
    return id == listing.id
        && propertyType.equals(listing.propertyType)
        && Objects.equals(city, listing.city)
        && Objects.equals(state, listing.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyType, id, city, state);
  }
}
