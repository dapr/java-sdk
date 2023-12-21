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

package io.dapr.grpc;

import io.dapr.v1.AppCallbackHealthCheckGrpc;
import io.dapr.v1.DaprAppCallbackProtos;

/**
 * Handles apps' health check callback.
 */
public class GrpcHealthCheckService extends AppCallbackHealthCheckGrpc.AppCallbackHealthCheckImplBase {

  /**
   * Handler for health check.
   * @param request Empty request.
   * @param responseObserver Response for gRPC response.
   */
  public void healthCheck(
      com.google.protobuf.Empty request,
      io.grpc.stub.StreamObserver<io.dapr.v1.DaprAppCallbackProtos.HealthCheckResponse> responseObserver) {
    responseObserver.onNext(DaprAppCallbackProtos.HealthCheckResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
