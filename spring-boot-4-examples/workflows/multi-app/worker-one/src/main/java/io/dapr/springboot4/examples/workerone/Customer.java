/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.springboot4.examples.workerone;

public class Customer {
  private String customerName;
  private String workflowId;
  private boolean inCustomerDB = false;
  private boolean followUp = false;

  public boolean isFollowUp() {
    return followUp;
  }

  public void setFollowUp(boolean followUp) {
    this.followUp = followUp;
  }

  public boolean isInCustomerDB() {
    return inCustomerDB;
  }

  public void setInCustomerDB(boolean inCustomerDB) {
    this.inCustomerDB = inCustomerDB;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  @Override
  public String toString() {
    return "Customer [customerName=" + customerName + ", workflowId=" + workflowId + ", inCustomerDB="
            + inCustomerDB + ", followUp=" + followUp + "]";
  }
}
