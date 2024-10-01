package io.dapr.spring.messaging.observation;

import io.micrometer.common.KeyValues;

/**
 * Default {@link DefaultDaprTemplateObservationConvention} for Pulsar template key values.
 *
 */
public class DefaultDaprTemplateObservationConvention implements DaprTemplateObservationConvention {

  /**
   * A singleton instance of the convention.
   */
  public static final DefaultDaprTemplateObservationConvention INSTANCE =
          new DefaultDaprTemplateObservationConvention();

  @Override
  public KeyValues getLowCardinalityKeyValues(DaprMessageSenderContext context) {
    return KeyValues.of(DaprTemplateObservation.TemplateLowCardinalityTags.BEAN_NAME.asString(),
            context.getBeanName());
  }

  // Remove once addressed:
  // https://github.com/micrometer-metrics/micrometer-docs-generator/issues/30
  @Override
  public String getName() {
    return "spring.dapr.template";
  }

  @Override
  public String getContextualName(DaprMessageSenderContext context) {
    return context.getDestination() + " send";
  }

}
