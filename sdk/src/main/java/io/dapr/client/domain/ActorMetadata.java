/*
 * Copyright 2024 The Dapr Authors
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

/**
 * ActorMetadata describes a registered Dapr Actor.
 */
public final class ActorMetadata {
  private final String type;
  private final int count;

  /**
   * Constructor for a ActorMetadata.
   *
   * @param type of the actor
   * @param count number of actors of a particular type
   */
  public ActorMetadata(String type, int count) {
    this.type = type;
    this.count = count;
  }

  public String getType() {
    return type;
  }

  public int getCount() {
    return count;
  }
}
