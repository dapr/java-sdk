package io.dapr.spring.data.observation;

import io.micrometer.common.KeyValues;

/**
 * Default {@link DefaultDaprKeyValueTemplateObservationConvention} for Dapr template key values.
 *
 */
public class DefaultDaprKeyValueTemplateObservationConvention implements DaprKeyValueTemplateObservationConvention {

  /**
   * A singleton instance of the convention.
   */
  public static final DefaultDaprKeyValueTemplateObservationConvention INSTANCE =
          new DefaultDaprKeyValueTemplateObservationConvention();

  @Override
  public KeyValues getLowCardinalityKeyValues(DaprKeyValueContext context) {
    return KeyValues.of(DaprKeyValueTemplateObservation.TemplateLowCardinalityTags.BEAN_NAME.asString(),
            context.getBeanName());
  }

  // Remove once addressed:
  // https://github.com/micrometer-metrics/micrometer-docs-generator/issues/30
  @Override
  public String getName() {
    return "spring.dapr.data.template";
  }

  @Override
  public String getContextualName(DaprKeyValueContext context) {
    return context.getKeyValueStore() + " store";
  }

}
