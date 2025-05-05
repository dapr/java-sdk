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

public class TestSenderWorkflowPayload extends TestWorkflowPayload {
  private String sendToworkflowId;

  public TestSenderWorkflowPayload() {
    super();
  }

  public TestSenderWorkflowPayload(List<String> payloads, String workflowId) {
    super(payloads, workflowId);
  }

  public TestSenderWorkflowPayload(String sendToworkflowId, List<String> payloads) {
    super(payloads);
    this.sendToworkflowId = sendToworkflowId;
  }

  public String getSendToworkflowId() {
    return sendToworkflowId;
  }

  public void setSendToworkflowId(String sendToworkflowId) {
    this.sendToworkflowId = sendToworkflowId;
  }
}
