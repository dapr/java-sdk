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

package io.dapr.client.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A request to save states to state store.
 * Deprecated in favor of @see{@link SaveStateRequest}.
 * Deprecated since SDK version 1.3.0, slated for removal in SDK version 1.5.0
 */
@Deprecated
public class SaveStateRequestBuilder {

  private final String storeName;

  private List<State<?>> states = new ArrayList<>();

  public SaveStateRequestBuilder(String storeName) {
    this.storeName = storeName;
  }

  public SaveStateRequestBuilder withStates(State<?>... states) {
    this.states = Collections.unmodifiableList(Arrays.asList(states));
    return this;
  }

  public SaveStateRequestBuilder withStates(List<State<?>> states) {
    this.states = states == null ? null : Collections.unmodifiableList(states);
    return this;
  }

  /**
   * Builds a request object.
   *
   * @return Request object.
   */
  public SaveStateRequest build() {
    SaveStateRequest request = new SaveStateRequest(storeName);
    return request.setStates(this.states);
  }
}
