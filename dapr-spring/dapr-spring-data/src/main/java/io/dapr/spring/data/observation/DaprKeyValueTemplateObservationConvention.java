package io.dapr.spring.data.observation;


import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;

/**
 * {@link ObservationConvention} for Dapr KV template .
 *
 */
public interface DaprKeyValueTemplateObservationConvention extends ObservationConvention<DaprKeyValueContext> {

  @Override
  default boolean supportsContext(Context context) {
    return context instanceof DaprKeyValueContext;
  }

  @Override
  default String getName() {
    return "spring.dapr.data.template";
  }

}
