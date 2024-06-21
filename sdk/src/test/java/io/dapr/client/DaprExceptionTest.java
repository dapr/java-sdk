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

package io.dapr.client;

import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import com.google.rpc.ResourceInfo;
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static io.dapr.client.DaprClientGrpcTest.newStatusRuntimeException;
import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprExceptionTest {
    private GrpcChannelFacade channel;
    private DaprGrpc.DaprStub daprStub;
    private DaprHttp daprHttp;
    private DaprClient client;

    @BeforeEach
    public void setup() throws IOException {
        channel = mock(GrpcChannelFacade.class);
        daprStub = mock(DaprGrpc.DaprStub.class);
        daprHttp = mock(DaprHttp.class);
        when(daprStub.withInterceptors(any())).thenReturn(daprStub);
        client = new DaprClientImpl(
                channel, daprStub, daprHttp, new DefaultObjectSerializer(), new DefaultObjectSerializer());
        doNothing().when(channel).close();
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.close();
        verify(channel).close();
    }

    @Test
    public void daprExceptionWithMultipleDetailsThrownTest() {
        ErrorInfo errorInfo = ErrorInfo.newBuilder()
                .setDomain("dapr.io")
                .setReason("fake")
                .build();

        ResourceInfo resourceInfo = ResourceInfo.newBuilder()
                .setResourceName("")
                .setResourceType("pubsub")
                .setDescription("pubsub name is empty")
                .build();

        com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                .setCode(io.grpc.Status.Code.INVALID_ARGUMENT.value())
                .setMessage("bad bad argument")
                .addDetails(Any.pack(errorInfo))
                .addDetails(Any.pack(resourceInfo))
                .build();

        doAnswer((Answer<Void>) invocation -> {
            throw newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument", status);
        }).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());

        DaprErrorDetails expectedStatusDetails = new DaprErrorDetails(status);

        assertThrowsDaprException(
                StatusRuntimeException.class,
                "INVALID_ARGUMENT",
                "INVALID_ARGUMENT: bad bad argument",
                expectedStatusDetails,
                () -> client.publishEvent("pubsubname","topic", "object").block());
    }

    @Test
    public void daprExceptionWithOneDetailThrownTest() {
        ErrorInfo errorInfo = ErrorInfo.newBuilder()
                .setDomain("dapr.io")
                .setReason("DAPR_STATE_NOT_FOUND")
                .build();

        com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                .setCode(io.grpc.Status.Code.INVALID_ARGUMENT.value())
                .setMessage("bad bad argument")
                .addDetails(Any.pack(errorInfo))
                .build();

        doAnswer((Answer<Void>) invocation -> {
            throw newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument", status);
        }).when(daprStub).getState(any(DaprProtos.GetStateRequest.class), any());

        DaprErrorDetails expectedStatusDetails = new DaprErrorDetails(status);

        assertThrowsDaprException(
                StatusRuntimeException.class,
                "INVALID_ARGUMENT",
                "INVALID_ARGUMENT: bad bad argument",
                expectedStatusDetails,
                () -> client.getState("Unknown state store", "myKey", String.class).block());
    }
}