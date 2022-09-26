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

package io.dapr.client.domain;

import java.util.List;

/**
 * Class representing the response returned on bulk publishing events.
 */
public class BulkPublishResponse {

  private String errorCode;
  private List<BulkPublishResponseEntry> statuses;

  /**
   * Default constructor for the class.
   */
  public BulkPublishResponse() {
  }

  /**
   * Constructor for the BulkPublishResponse object.
   *
   * @param errorCode Dapr errorCode if any.
   * @param statuses  The List of BulkPublishResponseEntries representing status of each event in bulk publish.
   */
  public BulkPublishResponse(String errorCode, List<BulkPublishResponseEntry> statuses) {
    this.errorCode = errorCode;
    this.statuses = statuses;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public List<BulkPublishResponseEntry> getStatuses() {
    return statuses;
  }

  public void setStatuses(List<BulkPublishResponseEntry> statuses) {
    this.statuses = statuses;
  }
}
