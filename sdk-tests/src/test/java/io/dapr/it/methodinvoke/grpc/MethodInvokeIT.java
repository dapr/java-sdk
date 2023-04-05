package io.dapr.it.methodinvoke.grpc;

import io.dapr.client.DaprApiProtocol;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

import static io.dapr.it.MethodInvokeServiceProtos.DeleteMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesRequest;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesResponse;
import static io.dapr.it.MethodInvokeServiceProtos.PostMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MethodInvokeIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    @Before
    public void init() throws Exception {
        daprRun = startDaprApp(
          MethodInvokeIT.class.getSimpleName(),
          MethodInvokeService.SUCCESS_MESSAGE,
          MethodInvokeService.class,
          DaprApiProtocol.GRPC,  // appProtocol
          60000);
        daprRun.switchToGRPC();
        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }

    @Test
    public void testInvoke() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            for (int i = 0; i < NUM_MESSAGES; i++) {
                String message = String.format("This is message #%d", i);

                PostMessageRequest req = PostMessageRequest.newBuilder().setId(i).setMessage(message).build();
                client.invokeMethod(daprRun.getAppName(), "postMessage", req, HttpExtension.POST).block();
                System.out.println("Invoke method messages : " + message);
            }

            Map<Integer, String> messages = client.invokeMethod(
                daprRun.getAppName(),
                "getMessages",
                GetMessagesRequest.newBuilder().build(),
                HttpExtension.POST, GetMessagesResponse.class).block().getMessagesMap();
            assertEquals(10, messages.size());

            // Delete one message.
            client.invokeMethod(
                daprRun.getAppName(),
                "deleteMessage",
                DeleteMessageRequest.newBuilder().setId(1).build(),
                HttpExtension.POST).block();
            messages = client.invokeMethod(
                daprRun.getAppName(),
                "getMessages",
                GetMessagesRequest.newBuilder().build(),
                HttpExtension.POST, GetMessagesResponse.class).block().getMessagesMap();
            assertEquals(9, messages.size());

            // Now update one message.
            client.invokeMethod(
                daprRun.getAppName(),
                "postMessage",
                PostMessageRequest.newBuilder().setId(2).setMessage("updated message").build(),
                HttpExtension.POST).block();
            messages = client.invokeMethod(
                daprRun.getAppName(),
                "getMessages",
                GetMessagesRequest.newBuilder().build(),
                HttpExtension.POST, GetMessagesResponse.class).block().getMessagesMap();
            assertEquals("updated message", messages.get(2));
        }
    }

    @Test
    public void testInvokeTimeout() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            long started = System.currentTimeMillis();
            SleepRequest req = SleepRequest.newBuilder().setSeconds(1).build();
            String message = assertThrows(IllegalStateException.class, () ->
                client.invokeMethod(daprRun.getAppName(), "sleep", req.toByteArray(), HttpExtension.POST)
                    .block(Duration.ofMillis(10))).getMessage();
            long delay = System.currentTimeMillis() - started;
            assertTrue(delay <= 500);  // 500 ms is a reasonable delay if the request timed out.
            assertEquals("Timeout on blocking read for 10000000 NANOSECONDS", message);
        }
    }

    @Test
    public void testInvokeException() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            SleepRequest req = SleepRequest.newBuilder().setSeconds(-9).build();
            DaprException exception = assertThrows(DaprException.class, () ->
                client.invokeMethod(daprRun.getAppName(), "sleep", req.toByteArray(), HttpExtension.POST).block());

            assertEquals("INTERNAL", exception.getErrorCode());
            assertEquals("INTERNAL: fail to invoke, id: methodinvokeit-methodinvokeservice, err: message is nil",
                exception.getMessage());
        }
    }
}
