package io.dapr.spring.data.observation;

import io.micrometer.observation.transport.SenderContext;

import java.util.HashMap;
import java.util.Map;


/**
 * {@link SenderContext} for Dapr KeyValue context.
 *
 */
public final class DaprKeyValueContext extends SenderContext<DaprKeyValueContext.KeyValueHolder> {

  private final String beanName;

  private final String keyValueStore;


  private DaprKeyValueContext(KeyValueHolder keyValueHolder, String keyValueStore, String beanName) {
    super((carrier, key, value) -> keyValueHolder.property(key, value));
    setCarrier(keyValueHolder);
    this.beanName = beanName;
    this.keyValueStore = keyValueStore;
  }

  /**
   * Create a new context.
   * @param kvStore KVStore to be used
   * @param beanName name of the bean used usually (typically a {@code DaprMessagingTemplate})
   * @return DaprMessageSenderContext
   */
  public static DaprKeyValueContext newContext(String kvStore, String beanName) {
    KeyValueHolder keyValueHolder = new KeyValueHolder();
    return new DaprKeyValueContext(keyValueHolder, kvStore, beanName);
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
  public String getKeyValueStore() {
    return this.keyValueStore;
  }


  /**
   * Acts as a carrier for a Dapr KeyValue and records the propagated properties for
   * later access by the Dapr.
   */
  public static final class KeyValueHolder {

    private final Map<String, String> properties = new HashMap<>();

    private KeyValueHolder() {
    }

    public void property(String key, String value) {
      this.properties.put(key, value);
    }

    public Map<String, String> properties() {
      return this.properties;
    }

  }

}
