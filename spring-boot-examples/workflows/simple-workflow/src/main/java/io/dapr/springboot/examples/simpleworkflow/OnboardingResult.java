/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.springboot.examples.simpleworkflow;

public class OnboardingResult {
  private String customerId;
  private String accountId;
  private boolean welcomeEmailSent;

  public OnboardingResult() {
  }

  /**
   * Creates a new OnboardingResult.
   *
   * @param customerId       id of the onboarded customer
   * @param accountId        id of the account created for the customer
   * @param welcomeEmailSent whether the welcome email was sent
   */
  public OnboardingResult(String customerId, String accountId, boolean welcomeEmailSent) {
    this.customerId = customerId;
    this.accountId = accountId;
    this.welcomeEmailSent = welcomeEmailSent;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public boolean isWelcomeEmailSent() {
    return welcomeEmailSent;
  }

  public void setWelcomeEmailSent(boolean welcomeEmailSent) {
    this.welcomeEmailSent = welcomeEmailSent;
  }

  @Override
  public String toString() {
    return "OnboardingResult{" + "customerId='" + customerId + '\'' + ", accountId='" + accountId + '\''
        + ", welcomeEmailSent=" + welcomeEmailSent + '}';
  }
}
