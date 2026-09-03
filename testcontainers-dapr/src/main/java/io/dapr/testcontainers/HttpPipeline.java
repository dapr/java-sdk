/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.testcontainers;

import java.util.Collections;
import java.util.List;

public class HttpPipeline implements ConfigurationSettings {
  private List<ListEntry> handlers;

  /**
   * Creates an HttpPipeline.
   *
   * @param handlers List of handlers for the HttpPipeline
   */
  public HttpPipeline(List<ListEntry> handlers) {
    if (handlers != null) {
      this.handlers = Collections.unmodifiableList(handlers);
    }
  }

  public List<ListEntry> getHandlers() {
    return handlers;
  }

}
