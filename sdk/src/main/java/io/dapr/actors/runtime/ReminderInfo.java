// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
// ------------------------------------------------------------

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapType;
import java.io.IOException;
import java.time.*;
import java.util.Base64;
import java.util.Map;

class ReminderInfo
{
  private final Duration minTimePeriod = Duration.ofMillis(-1);

  public Duration dueTime;
  public Duration period;
  public byte[] data;

  public ReminderInfo() {
  }

  public ReminderInfo(byte[] state, Duration dueTime, Duration period) {
    this.ValidateDueTime("DueTime", dueTime);
    this.ValidatePeriod("Period", period);
    this.data = state;
    this.dueTime = dueTime;
    this.period = period;
  }

  Duration getDueTime() {
    return this.dueTime;
  }

  Duration getPeriod() {
    return this.period;
  }

  byte[] getData() {
    return this.data;
  }

  String serialize() throws IOException {
    try {
      ObjectMapper om = new ObjectMapper();
      ObjectNode objectNode = om.createObjectNode();
      objectNode.put("dueTime", ConverterUtils.ConvertDurationToDaprFormat(this.dueTime));
      objectNode.put("period", ConverterUtils.ConvertDurationToDaprFormat(this.period));
      if (this.data != null) {
        objectNode.put("data", Base64.getEncoder().encodeToString(this.data));
      }

      return om.writeValueAsString(objectNode);
    } catch (IOException e) {
      throw e;
    }
  }

  static ReminderInfo deserialize(byte[] stream) throws IOException {
    try {
      ObjectMapper om = new ObjectMapper();
      MapType type = om.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
      Map<String, Object> data = om.readValue(stream, type);

      String d = (String)data.getOrDefault("dueTime", "");
      Duration dueTime = ConverterUtils.ConvertDurationFromDaprFormat(d);

      String p = (String)data.getOrDefault("period", "");
      Duration period = ConverterUtils.ConvertDurationFromDaprFormat(p);

      String s = (String)data.getOrDefault("data", null);
      byte[] state = (s == null) ? null : Base64.getDecoder().decode(s);

      return new ReminderInfo(state, dueTime, period);
    } catch (IOException e) {
      throw e;
    }
  }

  private void ValidateDueTime(String argName, Duration value)
  {
    if (value.compareTo(Duration.ZERO) < 0 )
    {
        String message = String.format("argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
        throw new IllegalArgumentException(message);
    }
  }

  private void ValidatePeriod(String argName, Duration value) throws IllegalArgumentException
  {
    if (value.compareTo(this.minTimePeriod) < 0)
    {
        String message = String.format("argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
        throw new IllegalArgumentException(message);
    }
  }
}
