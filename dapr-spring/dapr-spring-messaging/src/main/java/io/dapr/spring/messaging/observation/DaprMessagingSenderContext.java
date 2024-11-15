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

import io.micrometer.observation.transport.SenderContext;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link SenderContext} for Dapr Messaging.
 *
 */
public final class DaprMessagingSenderContext extends SenderContext<DaprMessagingSenderContext.Carrier> {
  private final String beanName;

  private final String destination;

  private DaprMessagingSenderContext(Carrier dataHolder, String topic, String beanName) {
    super((carrier, key, value) -> dataHolder.property(key, value));
    setCarrier(dataHolder);
    this.beanName = beanName;
    this.destination = topic;
  }

  /**
   * Create a new context.
   * @param topic topic to be used
   * @param beanName name of the bean used usually (typically a {@code DaprMessagingTemplate})
   * @return DaprMessageSenderContext
   */
  public static DaprMessagingSenderContext newContext(String topic, String beanName) {
    Carrier carrier = new Carrier();
    return new DaprMessagingSenderContext(carrier, topic, beanName);
  }

  /**
   * The properties of the message.
   * @return the properties of the message
   */
  public Map<String, String> properties() {
    Carrier carrier = getCarrier();

    if (carrier == null) {
      return Map.of();
    }

    return carrier.properties();
  }


  /**
   * The name of the bean sending the message (typically a {@code DaprMessagingTemplate}).
   * @return the name of the bean sending the message
   */
  public String getBeanName() {
    return this.beanName;
  }

  /**
   * The destination topic for the message.
   * @return the topic the message is being sent to
   */
  public String getDestination() {
    return this.destination;
  }


  /**
   * Acts as a carrier for a Dapr message and records the propagated properties for
   * later access by the Dapr.
   */
  public static final class Carrier {

    private final Map<String, String> properties = new HashMap<>();

    private Carrier() {
    }

    public void property(String key, String value) {
      this.properties.put(key, value);
    }

    public Map<String, String> properties() {
      return Map.copyOf(this.properties);
    }
  }
}
