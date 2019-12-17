package io.dapr.actors.runtime;

import org.json.JSONObject;
import java.time.Duration;
import java.util.function.Function;

/**
 * Represents the timer set on an Actor.
 */
class ActorTimerImpl implements ActorTimer {

  private final AbstractActor owner;
  private String name;
  private Function<Object, Void> asyncCallback;
  private Object state;
  private Duration dueTime;
  private Duration period;

  /**
   *
   * @param owner  The Actor that owns this timer.  The timer callback will be fired for this Actor.
   * @param timerName The name of the timer.
   * @param asyncCallback The callback to invoke when the timer fires.
   * @param state information to be used by the callback method
   * @param dueTime the time when timer is first due.
   * @param period the periodic time when timer will be invoked.
   */
  public ActorTimerImpl(AbstractActor owner, String timerName, Function<Object, Void> asyncCallback, Object state, Duration dueTime, Duration period) {
    this.owner = owner;
    this.name = timerName;
    this.asyncCallback = asyncCallback;
    this.state = state;
    this.dueTime = dueTime;
    this.period = period;
  }

  /**
   * Gets the name of the Timer. The name is unique per actor.
   * @return The name of the timer.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the time when timer is first due.
   * @return Time as Duration when timer is first due.
   */
  public Duration getDueTime() {
    return this.dueTime;
  }

  /**
   * @return Gets a delegate that specifies a method to be called when the timer fires.
   *     It has one parameter: the state object passed to RegisterTimer.
   */
  public Function<Object, Void> getAsyncCallback() {
    return this.asyncCallback;
  }

  /**
   * Gets the periodic time when timer will be invoked.
   * @return Periodic time as Duration when timer will be invoked.
   */
  public Duration getPeriod() {
    return this.period;
  }

  /**
   *
   * @return Gets state containing information to be used by the callback method, or null.
   */
  public Object getState() {
    return this.state;
  }

  /**
   *
   * @return
   */
  String serialize()
  {
    JSONObject j = new JSONObject();
    j.put("dueTime", ConverterUtils.ConvertDurationToDaprFormat(this.getDueTime()));
    j.put("period", ConverterUtils.ConvertDurationToDaprFormat(this.getPeriod()));
    return j.toString();
  }
}
