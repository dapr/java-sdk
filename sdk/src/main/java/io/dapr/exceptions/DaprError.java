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

package io.dapr.exceptions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.rpc.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an error message from Dapr.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DaprError {

  /**
   * Error code.
   */
  private String errorCode;

  /**
   * Error Message.
   */
  private String message;

  /**
   * Error code from gRPC.
   */
  private Integer code;

  /**
   * Error status details.
   */
  private List<Map<String, Object>> statusDetails;

  /**
   * Gets the error code.
   *
   * @return Error code.
   */
  public String getErrorCode() {
    if ((errorCode == null) && (code != null)) {
      return io.grpc.Status.fromCodeValue(code).getCode().name();
    }
    return errorCode;
  }

  /**
   * Sets the error code.
   *
   * @param errorCode Error code.
   * @return This instance.
   */
  public DaprError setErrorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  /**
   * Gets the error message.
   *
   * @return Error message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the error message.
   *
   * @param message Error message.
   * @return This instance.
   */
  public DaprError setMessage(String message) {
    this.message = message;
    return this;
  }

  /**
   * Sets the status details for the error.
   *
   * @param statusDetails Status details to set.
   * @return This instance.
   */
  public DaprError setStatusDetails(Status statusDetails) {
    if (statusDetails != null) {
      this.statusDetails = parseStatusDetails(statusDetails);
    }
    return this;
  }

  /**
   * Gets the status details for the error.
   *
   * @return Status details.
   */
  public List<Map<String, Object>> getStatusDetails() {
    return statusDetails;
  }

  /**
   * Parses status details from a gRPC Status.
   *
   * @param status The gRPC Status to parse details from.
   * @return List containing parsed status details.
   */
  public static List<Map<String, Object>> parseStatusDetails(Status status) {
    List<Map<String, Object>> detailsList = new ArrayList<>();
    if (status == null || status.getDetailsList() == null) {
      return detailsList;
    }

    List<Any> grpcDetailsList = status.getDetailsList();
    for (Any detail : grpcDetailsList) {
      try {
        if (detail.is(com.google.rpc.ErrorInfo.class)) {
          com.google.rpc.ErrorInfo errorInfo = detail.unpack(com.google.rpc.ErrorInfo.class);
          detailsList.add(ProtoMessageConverter.messageToMap(errorInfo));
        }
        if (detail.is(com.google.rpc.RetryInfo.class)) {
          com.google.rpc.RetryInfo retryInfo = detail.unpack(com.google.rpc.RetryInfo.class);
          detailsList.add(ProtoMessageConverter.messageToMap(retryInfo));
        }
        if (detail.is(com.google.rpc.DebugInfo.class)) {
          com.google.rpc.DebugInfo debugInfo = detail.unpack(com.google.rpc.DebugInfo.class);
          detailsList.add(ProtoMessageConverter.messageToMap(debugInfo));
        }
        if (detail.is(com.google.rpc.QuotaFailure.class)) {
          com.google.rpc.QuotaFailure quotaFailure = detail.unpack(com.google.rpc.QuotaFailure.class);
          detailsList.add(ProtoMessageConverter.messageToMap(quotaFailure));
        }
        if (detail.is(com.google.rpc.PreconditionFailure.class)) {
          com.google.rpc.PreconditionFailure preconditionFailure = detail.unpack(
                  com.google.rpc.PreconditionFailure.class);
          detailsList.add(ProtoMessageConverter.messageToMap(preconditionFailure));
        }
        if (detail.is(com.google.rpc.BadRequest.class)) {
          com.google.rpc.BadRequest badRequest = detail.unpack(com.google.rpc.BadRequest.class);
          detailsList.add(ProtoMessageConverter.messageToMap(badRequest));
        }
        if (detail.is(com.google.rpc.RequestInfo.class)) {
          com.google.rpc.RequestInfo requestInfo = detail.unpack(com.google.rpc.RequestInfo.class);
          detailsList.add(ProtoMessageConverter.messageToMap(requestInfo));
        }
        if (detail.is(com.google.rpc.ResourceInfo.class)) {
          com.google.rpc.ResourceInfo resourceInfo = detail.unpack(com.google.rpc.ResourceInfo.class);
          detailsList.add(ProtoMessageConverter.messageToMap(resourceInfo));
        }
        if (detail.is(com.google.rpc.Help.class)) {
          com.google.rpc.Help help = detail.unpack(com.google.rpc.Help.class);
          detailsList.add(ProtoMessageConverter.messageToMap(help));
        }
        if (detail.is(com.google.rpc.LocalizedMessage.class)) {
          com.google.rpc.LocalizedMessage localizedMessage = detail.unpack(com.google.rpc.LocalizedMessage.class);
          detailsList.add(ProtoMessageConverter.messageToMap(localizedMessage));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return detailsList;
  }
}
