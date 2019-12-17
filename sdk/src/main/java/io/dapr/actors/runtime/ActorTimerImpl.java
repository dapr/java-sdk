package io.dapr.actors.runtime;

import org.json.JSONObject;

import java.time.Duration;
import java.util.function.Function;

class ActorTimerImpl implements ActorTimer {

  private final AbstractActor owner;
  private String name;
  private Function<Object, Void> asyncCallback;
  private Object state;
  private Duration dueTime;
  private Duration period;

  public ActorTimerImpl(AbstractActor owner, String timerName, Function<Object, Void> asyncCallback, Object state, Duration dueTime, Duration period) {
    this.owner = owner;
    this.name = timerName;
    this.asyncCallback = asyncCallback;
    this.state = state;
    this.dueTime = dueTime;
    this.period = period;
  }

  public String getName() {
    return this.name;
  }

  public Duration getDueTime() {
    return this.dueTime;
  }

  public Function<Object, Void> getAsyncCallback() {
    return this.asyncCallback;
  }

  public Duration getPeriod() {
    return this.period;
  }

  public Object getState() {
    return this.state;
  }

  String serialize()
  {
    JSONObject j = new JSONObject();
    j.put("dueTime", ConverterUtils.ConvertDurationToDaprFormat(this.getDueTime()));
    j.put("period", ConverterUtils.ConvertDurationToDaprFormat(this.getPeriod()));
    return j.toString();
  }
}
