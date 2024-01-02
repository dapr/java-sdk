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

package io.dapr.config;

import java.time.Duration;

/**
 * Integer configuration property.
 */
public class MillisecondsDurationProperty extends Property<Duration> {

  /**
   * {@inheritDoc}
   */
  MillisecondsDurationProperty(String name, String envName, Duration defaultValue) {
    super(name, envName, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Duration parse(String value) {
    long longValue = Long.parseLong(value);
    return Duration.ofMillis(longValue);
  }

}
