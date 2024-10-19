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

package io.dapr.spring.messaging.observation;

import io.micrometer.common.KeyValues;

/**
 * Default {@link DefaultDaprMessagingObservationConvention} for Dapr template key values.
 *
 */
public class DefaultDaprMessagingObservationConvention implements DaprMessagingObservationConvention {
  /**
   * A singleton instance of the convention.
   */
  public static final DefaultDaprMessagingObservationConvention INSTANCE =
      new DefaultDaprMessagingObservationConvention();

  @Override
  public KeyValues getLowCardinalityKeyValues(DaprMessagingSenderContext context) {
    return KeyValues.of(DaprMessagingObservationDocumentation.TemplateLowCardinalityTags.BEAN_NAME.asString(),
        context.getBeanName());
  }

  // Remove once addressed:
  // https://github.com/micrometer-metrics/micrometer-docs-generator/issues/30
  @Override
  public String getName() {
    return "spring.dapr.messaging.template";
  }

  @Override
  public String getContextualName(DaprMessagingSenderContext context) {
    return context.getDestination() + " send";
  }

}
