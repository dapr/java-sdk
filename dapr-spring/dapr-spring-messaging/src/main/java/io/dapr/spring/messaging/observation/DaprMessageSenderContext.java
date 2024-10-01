package io.dapr.spring.messaging.observation;

import io.micrometer.observation.transport.SenderContext;
import java.util.HashMap;
import java.util.Map;


/**
 * {@link SenderContext} for Dapr messages.
 *
 */
public final class DaprMessageSenderContext extends SenderContext<DaprMessageSenderContext.MessageHolder> {

  private final String beanName;

  private final String destination;


  private DaprMessageSenderContext(MessageHolder messageHolder, String topic, String beanName) {
    super((carrier, key, value) -> messageHolder.property(key, value));
    setCarrier(messageHolder);
    this.beanName = beanName;
    this.destination = topic;
  }

  /**
   * Create a new context.
   * @param topic topic to be used
   * @param beanName name of the bean used usually (typically a {@code DaprMessagingTemplate})
   * @return DaprMessageSenderContext
   */
  public static DaprMessageSenderContext newContext(String topic, String beanName) {
    MessageHolder messageHolder = new MessageHolder();
    return new DaprMessageSenderContext(messageHolder, topic, beanName);
  }

  public Map<String, String> properties() {
    return getCarrier().properties();
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
   * Acts as a carrier for a Pulsar message and records the propagated properties for
   * later access by the Pulsar message builder.
   */
  public static final class MessageHolder {

    private final Map<String, String> properties = new HashMap<>();

    private MessageHolder() {
    }

    public void property(String key, String value) {
      this.properties.put(key, value);
    }

    public Map<String, String> properties() {
      return this.properties;
    }

  }

}
