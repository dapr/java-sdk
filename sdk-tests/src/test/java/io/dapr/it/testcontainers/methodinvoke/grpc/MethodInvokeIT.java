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

package io.dapr.it.testcontainers.methodinvoke.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.it.MethodInvokeServiceGrpc;
import io.dapr.it.testcontainers.methodinvoke.grpc.app.MethodInvokeService;
import io.dapr.it.testcontainers.support.DaprTestEnvironment;
import io.dapr.testcontainers.DaprProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.dapr.it.MethodInvokeServiceProtos.DeleteMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesRequest;
import static io.dapr.it.MethodInvokeServiceProtos.PostMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("testcontainers")
@Execution(ExecutionMode.SAME_THREAD)
public class MethodInvokeIT {

    private static final String APP_NAME = "methodinvoke-grpc-it";
    private static final int NUM_MESSAGES = 10;
    private static final int TIMEOUT_MS = 100;
    private static final ResiliencyOptions RESILIENCY_OPTIONS = new ResiliencyOptions()
        .setTimeout(Duration.ofMillis(TIMEOUT_MS));

    @Container
    private static final DaprTestEnvironment ENVIRONMENT =
        new DaprTestEnvironment(APP_NAME, MethodInvokeService.class, DaprProtocol.GRPC);

    @BeforeEach
    public void reset() throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(ENVIRONMENT.appAddress()).usePlaintext().build();

        try {
            var stub = MethodInvokeServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS);
            var request = GetMessagesRequest.getDefaultInstance();

            for (int id : stub.getMessages(request).getMessagesMap().keySet()) {
                stub.deleteMessage(DeleteMessageRequest.newBuilder().setId(id).build());
            }

            assertTrue(stub.getMessages(request).getMessagesMap().isEmpty());
        } finally {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS), "reset channel did not terminate");
        }
    }

    @Test
    public void testInvoke() throws Exception {
        try (DaprClient client = ENVIRONMENT.clientBuilder().build()) {
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
        try (DaprClient client = ENVIRONMENT.clientBuilder().withResiliencyOptions(RESILIENCY_OPTIONS).build()) {
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
        try (DaprClient client = ENVIRONMENT.clientBuilder().build()) {
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
