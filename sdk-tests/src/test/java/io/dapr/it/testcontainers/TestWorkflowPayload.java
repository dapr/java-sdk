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

package io.dapr.it.testcontainers;

import java.util.List;

public class TestWorkflowPayload {
  private List<String> payloads;
  private String workflowId;

  public TestWorkflowPayload() {
  }

  public TestWorkflowPayload(List<String> payloads, String workflowId) {
    this.payloads = payloads;
    this.workflowId = workflowId;
  }

  public TestWorkflowPayload(List<String> payloads) {
    this.payloads = payloads;
  }

  public List<String> getPayloads() {
    return payloads;
  }

  public void setPayloads(List<String> payloads) {
    this.payloads = payloads;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
}
