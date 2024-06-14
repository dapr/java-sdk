package io.dapr.it.methodinvoke.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprClientGrpcInterceptors;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.dapr.it.AppRun;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.MethodInvokeServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
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

public class MethodInvokeIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    @BeforeEach
    public void init() throws Exception {
        daprRun = startDaprApp(
          MethodInvokeIT.class.getSimpleName() + "grpc",
          MethodInvokeService.SUCCESS_MESSAGE,
          MethodInvokeService.class,
          AppRun.AppProtocol.GRPC,  // appProtocol
          60000);
        daprRun.waitForAppHealth(40000);
    }

    @Test
    public void testInvoke() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub =
                MethodInvokeServiceGrpc.newBlockingStub(client.getGrpcChannel());
            stub = DaprClientGrpcInterceptors.intercept(daprRun.getAppName(), stub);
            client.waitForSidecar(10000).block();
            daprRun.waitForAppHealth(10000);
            
            for (int i = 0; i < NUM_MESSAGES; i++) {
                String message = String.format("This is message #%d", i);

                PostMessageRequest req = PostMessageRequest.newBuilder().setId(i).setMessage(message).build();
                stub.postMessage(req);

                System.out.println("Invoke method messages : " + message);
            }

            Map<Integer, String> messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
            assertEquals(10, messages.size());

            // Delete one message.
            stub.deleteMessage(DeleteMessageRequest.newBuilder().setId(1).build());
            messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
            assertEquals(9, messages.size());

            // Now update one message.
            stub.postMessage(PostMessageRequest.newBuilder().setId(2).setMessage("updated message").build());
            messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
            assertEquals("updated message", messages.get(2));
        }
    }

    @Test
    public void testInvokeTimeout() throws Exception {
        long timeoutMs = 100;
        try (DaprClient client = new DaprClientBuilder().build()) {
            final MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub =
                DaprClientGrpcInterceptors.intercept(daprRun.getAppName(),
                    MethodInvokeServiceGrpc.newBlockingStub(client.getGrpcChannel()),
                    new TimeoutPolicy(Duration.ofMillis(timeoutMs)));
            client.waitForSidecar(10000).block();
            daprRun.waitForAppHealth(10000);

            long started = System.currentTimeMillis();
            SleepRequest req = SleepRequest.newBuilder().setSeconds(1).build();
            String message = assertThrows(StatusRuntimeException.class, () -> stub.sleep(req)).getMessage();
            long delay = System.currentTimeMillis() - started;
            assertTrue(delay >= timeoutMs);
            assertTrue(message.startsWith("DEADLINE_EXCEEDED: deadline exceeded after"));
        }
    }

    @Test
    public void testInvokeException() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            client.waitForSidecar(10000).block();
            daprRun.waitForAppHealth(10000);

            final MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub =
                DaprClientGrpcInterceptors.intercept(daprRun.getAppName(),
                    MethodInvokeServiceGrpc.newBlockingStub(client.getGrpcChannel()));

            SleepRequest req = SleepRequest.newBuilder().setSeconds(-9).build();
            StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.sleep(req));

            // The error messages should be improved once runtime has standardized error serialization in the API.
            // This message is not ideal but last time it was improved, there was side effects reported by users.
            // If this test fails, there might be a regression in runtime (like we had in 1.10.0).
            // The expectations below are as per 1.9 release and (later on) hotfixed in 1.10.
            assertEquals(Status.UNKNOWN.getCode(), exception.getStatus().getCode());
            assertEquals("", exception.getStatus().getDescription());
        }
    }
}
