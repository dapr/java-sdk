/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.actors.runtime;

import java.time.Duration;

public class ReminderOptions {
    
  //required parameters
  private String name;

  //optional parameters
  private Duration dueTime;
  private Duration period;
    

  public String getName() {
    return name;
  }

  public Duration getDueTime() {
    return dueTime;
  }

  public Duration getPeriod() {
    return period;
  }
    
  private ReminderOptions(ReminderOptionsBuilder builder) {
    this.name = builder.name;
    this.dueTime = builder.dueTime;
    this.period = builder.period;
  }
    
  //Builder Class
  public static class ReminderOptionsBuilder<T> {

    //required parameters
    private String name;

    //optional parameters
    private Duration dueTime = Duration.ZERO;
    private Duration period = Duration.ZERO;
        
    public ReminderOptionsBuilder(String name) {
      this.name = name;
    }

    public ReminderOptionsBuilder setDueTime(Duration dueTime) {
      this.dueTime = dueTime;
      return this;
    }

    public ReminderOptionsBuilder setPeriod(Duration period) {
      this.period = period;
      return this;
    }
    
    public ReminderOptions build() {
      return new ReminderOptions(this);
    }

  }

}