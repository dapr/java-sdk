package io.dapr.spring.data.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * An {@link Observation} for {@link io.dapr.spring.data.DaprKeyValueTemplate}.
 *
 */
public enum DaprKeyValueTemplateObservation implements ObservationDocumentation {

  /**
   * Observation created when a Dapr template interacts with a KVStore.
   */
  TEMPLATE_OBSERVATION {

    @Override
    public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
      return DefaultDaprKeyValueTemplateObservationConvention.class;
    }

    @Override
    public String getPrefix() {
      return "spring.dapr.data.template";
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
      return TemplateLowCardinalityTags.values();
    }

  };

  /**
   * Low cardinality tags.
   */
  public enum TemplateLowCardinalityTags implements KeyName {

    /**
     * Bean name of the template that interacts with the kv store.
     */
    BEAN_NAME {

      @Override
      public String asString() {
        return "spring.dapr.data.template.name";
      }

    }

  }

}
