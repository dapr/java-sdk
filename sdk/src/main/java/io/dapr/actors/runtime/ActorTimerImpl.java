package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

/**
 * Represents the timer set on an Actor.
 */
class ActorTimerImpl implements ActorTimer {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation, used only for this class.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Actor that owns this timer.
   */
  private final AbstractActor owner;

  /**
   * Name of this timer.
   */
  private String name;

  /**
   * Async callbacks for the timer.
   */
  private Function<Object, Void> asyncCallback;

  /**
   * State to be sent in the timer.
   */
  private Object state;

  /**
   * Due time for the timer's first trigger.
   */
  private Duration dueTime;

  /**
   * Period at which the timer will be triggered.
   */
  private Duration period;

  /**
   * @param owner         The Actor that owns this timer.  The timer callback will be fired for this Actor.
   * @param timerName     The name of the timer.
   * @param asyncCallback The callback to invoke when the timer fires.
   * @param state         information to be used by the callback method
   * @param dueTime       the time when timer is first due.
   * @param period        the periodic time when timer will be invoked.
   */
  public ActorTimerImpl(AbstractActor owner,
                        String timerName,
                        Function<Object, Void> asyncCallback,
                        Object state,
                        Duration dueTime,
                        Duration period) {
    this.owner = owner;
    this.name = timerName;
    this.asyncCallback = asyncCallback;
    this.state = state;
    this.dueTime = dueTime;
    this.period = period;
  }

  /**
   * Gets the name of the Timer. The name is unique per actor.
   *
   * @return The name of the timer.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the time when timer is first due.
   *
   * @return Time as Duration when timer is first due.
   */
  public Duration getDueTime() {
    return this.dueTime;
  }

  /**
   * @return Gets a delegate that specifies a method to be called when the timer fires.
   * It has one parameter: the state object passed to RegisterTimer.
   */
  public Function<Object, Void> getAsyncCallback() {
    return this.asyncCallback;
  }

  /**
   * Gets the periodic time when timer will be invoked.
   *
   * @return Periodic time as Duration when timer will be invoked.
   */
  public Duration getPeriod() {
    return this.period;
  }

  /**
   * @return Gets state containing information to be used by the callback method, or null.
   */
  public Object getState() {
    return this.state;
  }

  /**
   * Generates JSON representation of this timer.
   *
   * @return JSON.
   */
  String serialize() throws IOException {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    objectNode.put("dueTime", ConverterUtils.ConvertDurationToDaprFormat(this.dueTime));
    objectNode.put("period", ConverterUtils.ConvertDurationToDaprFormat(this.period));
    return OBJECT_MAPPER.writeValueAsString(objectNode);
  }
}
