package io.dapr.spring.messaging.observation;


import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;

/**
 * {@link ObservationConvention} for Dapr template .
 *
 */
public interface DaprTemplateObservationConvention extends ObservationConvention<DaprMessageSenderContext> {

  @Override
  default boolean supportsContext(Context context) {
    return context instanceof DaprMessageSenderContext;
  }

  @Override
  default String getName() {
    return "spring.dapr.template";
  }

}
