/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.examples.workflows;

public class DemoActivityOutput {

  private String originalMessage;
  private String newMessage;

  public DemoActivityOutput() {
  }

  public DemoActivityOutput(String originalMessage, String newMessage) {
    this.originalMessage = originalMessage;
    this.newMessage = newMessage;
  }

  public String getOriginalMessage() {
    return originalMessage;
  }

  public void setOriginalMessage(String originalMessage) {
    this.originalMessage = originalMessage;
  }

  public String getNewMessage() {
    return newMessage;
  }

  public void setNewMessage(String newMessage) {
    this.newMessage = newMessage;
  }
}
