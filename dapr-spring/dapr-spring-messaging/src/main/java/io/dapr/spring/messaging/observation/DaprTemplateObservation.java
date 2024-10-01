package io.dapr.spring.messaging.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * An {@link Observation} for {@link io.dapr.spring.messaging.DaprMessagingTemplate}.
 *
 */
public enum DaprTemplateObservation implements ObservationDocumentation {

  /**
   * Observation created when a Dapr template sends a message.
   */
  TEMPLATE_OBSERVATION {

    @Override
    public Class<? extends ObservationConvention<? extends Context>> getDefaultConvention() {
      return DefaultDaprTemplateObservationConvention.class;
    }

    @Override
    public String getPrefix() {
      return "spring.dapr.template";
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
     * Bean name of the template that sent the message.
     */
    BEAN_NAME {

      @Override
      public String asString() {
        return "spring.dapr.template.name";
      }

    }

  }

}
