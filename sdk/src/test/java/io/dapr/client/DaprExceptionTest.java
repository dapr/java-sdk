package io.dapr.client;

import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import com.google.rpc.ResourceInfo;
import io.dapr.exceptions.DaprError;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.DaprGrpc;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.client.DaprClientGrpcTest.newStatusRuntimeException;
import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import io.dapr.v1.DaprProtos;

public class DaprExceptionTest {
    private GrpcChannelFacade channel;
    private DaprGrpc.DaprStub daprStub;
    private DaprClient client;
    private ObjectSerializer serializer;

    @BeforeEach
    public void setup() throws IOException {
        channel = mock(GrpcChannelFacade.class);
        daprStub = mock(DaprGrpc.DaprStub.class);
        when(daprStub.withInterceptors(any())).thenReturn(daprStub);
        DaprClient grpcClient = new DaprClientGrpc(
                channel, daprStub, new DefaultObjectSerializer(), new DefaultObjectSerializer());
        client = new DaprClientProxy(grpcClient);
        serializer = new ObjectSerializer();
        doNothing().when(channel).close();
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.close();
        channel.close();
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

        List<Map<String, Object>> detailsList = DaprError.parseStatusDetails(status);

        Map<String, Object> expectedStatusDetails = new HashMap<>();
        expectedStatusDetails.put("details", detailsList);


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

        List<Map<String, Object>> detailsList = DaprError.parseStatusDetails(status);

        Map<String, Object> expectedStatusDetails = new HashMap<>();
        expectedStatusDetails.put("details", detailsList);

        assertThrowsDaprException(
                StatusRuntimeException.class,
                "INVALID_ARGUMENT",
                "INVALID_ARGUMENT: bad bad argument",
                expectedStatusDetails,
                () -> client.getState("Unknown state store", "myKey", String.class).block());
    }
}