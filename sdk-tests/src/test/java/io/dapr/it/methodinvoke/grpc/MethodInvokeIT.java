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

package io.dapr.it.methodinvoke.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.it.AppRun;
import io.dapr.it.MethodInvokeServiceGrpc;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprProtocol;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static io.dapr.it.MethodInvokeServiceProtos.DeleteMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesRequest;
import static io.dapr.it.MethodInvokeServiceProtos.PostMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MethodInvokeIT extends BaseContainerIT {

    private static final String APP_NAME = "methodinvoke-grpc-it";
    private static final int NUM_MESSAGES = 10;
    private static final int TIMEOUT_MS = 100;
    private static final ResiliencyOptions RESILIENCY_OPTIONS = new ResiliencyOptions()
        .setTimeout(Duration.ofMillis(TIMEOUT_MS));

    private static DaprContainer dapr;
    private static AppRun app;

    @BeforeAll
    public static void init() throws Exception {
        var pair = startAppAndAttach(
            APP_NAME,
            MethodInvokeService.class,
            AppRun.AppProtocol.GRPC,
            appPort -> {
                DaprContainer d = daprBuilder(APP_NAME)
                    .withAppPort(appPort)
                    .withAppChannelAddress("host.testcontainers.internal")
                    .withAppProtocol(DaprProtocol.GRPC);
                return d;
            });
        dapr = pair.dapr();
        app = pair.app();
    }

    @Test
    public void testInvoke() throws Exception {
        try (DaprClient client = newDaprClient(dapr)) {
            client.waitForSidecar(10000).block();

            MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub = createGrpcStub(client);

            for (int i = 0; i < NUM_MESSAGES; i++) {
                String message = String.format("This is message #%d", i);
                PostMessageRequest req = PostMessageRequest.newBuilder().setId(i).setMessage(message).build();

                stub.postMessage(req);

                System.out.println("Invoke method messages : " + message);
            }

            Map<Integer, String> messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
            assertEquals(NUM_MESSAGES, messages.size());

            // Delete one message.
            stub.deleteMessage(DeleteMessageRequest.newBuilder().setId(1).build());
            messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
            assertEquals(NUM_MESSAGES - 1, messages.size());

            // Now update one message.
            stub.postMessage(PostMessageRequest.newBuilder().setId(2).setMessage("updated message").build());
            messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
            assertEquals("updated message", messages.get(2));
        }
    }

    @Test
    public void testInvokeTimeout() throws Exception {
        try (DaprClient client = newDaprClientBuilder(dapr).withResiliencyOptions(RESILIENCY_OPTIONS).build()) {
            client.waitForSidecar(10000).block();

            MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub = createGrpcStub(client);
            long started = System.currentTimeMillis();
            SleepRequest req = SleepRequest.newBuilder().setSeconds(1).build();
            StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.sleep(req));
            long delay = System.currentTimeMillis() - started;
            Status.Code code = exception.getStatus().getCode();

            assertTrue(delay >= TIMEOUT_MS, "Delay: " + delay + " is not greater than timeout: " + TIMEOUT_MS);
            assertEquals(Status.DEADLINE_EXCEEDED.getCode(), code, "Expected timeout error");
        }
    }

    @Test
    public void testInvokeException() throws Exception {
        try (DaprClient client = newDaprClient(dapr)) {
            client.waitForSidecar(10000).block();

            MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub = createGrpcStub(client);

            SleepRequest req = SleepRequest.newBuilder().setSeconds(-9).build();
            StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.sleep(req));

            // The error messages should be improved once runtime has standardized error serialization in the API.
            // This message is not ideal but last time it was improved, there was side effects reported by users.
            // If this test fails, there might be a regression in runtime (like we had in 1.10.0).
            // The expectations below are as per 1.9 release and (later on) hotfixed in 1.10.
            assertEquals(Status.UNKNOWN.getCode(), exception.getStatus().getCode());
            // The error message below is added starting in Dapr 1.15.0
            assertEquals("Application error processing RPC", exception.getStatus().getDescription());
        }
    }

    private static MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub createGrpcStub(DaprClient client) {
        return client.newGrpcStub(APP_NAME, MethodInvokeServiceGrpc::newBlockingStub);
    }
}
