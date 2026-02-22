package io.dapr.it.methodinvoke.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.it.MethodInvokeServiceGrpc;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprProtocol;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.utils.NetworkUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static io.dapr.it.MethodInvokeServiceProtos.DeleteMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.GetMessagesRequest;
import static io.dapr.it.MethodInvokeServiceProtos.PostMessageRequest;
import static io.dapr.it.MethodInvokeServiceProtos.SleepRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("testcontainers")
public class MethodInvokeIT {

  private static final String APP_ID = "methodinvoke-grpc";

  // Number of messages to be sent: 10
  private static final int NUM_MESSAGES = 10;
  private static final int TIMEOUT_MS = 100;
  private static final ResiliencyOptions RESILIENCY_OPTIONS = new ResiliencyOptions()
      .setTimeout(Duration.ofMillis(TIMEOUT_MS));

  @Container
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory
      .createForSpringBootTest(APP_ID)
      .withAppProtocol(DaprProtocol.GRPC);

  private DaprClient daprClient;

  @BeforeAll
  public static void startGrpcApp() throws Exception {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    Thread appThread = new Thread(() -> {
      try {
        MethodInvokeService.main(new String[] {String.valueOf(DAPR_CONTAINER.getAppPort())});
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    });
    appThread.setDaemon(true);
    appThread.start();
    NetworkUtils.waitForSocket("127.0.0.1", DAPR_CONTAINER.getAppPort(), 60000);
  }

  @BeforeEach
  public void init() {
    daprClient = new DaprClientBuilderFactory().newBuilder(DAPR_CONTAINER).build();
    daprClient.waitForSidecar(10000).block();
  }

  @AfterEach
  public void closeClient() throws Exception {
    daprClient.close();
  }

  @Test
  public void testInvoke() {
    MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub = createGrpcStub(daprClient);

    for (int i = 0; i < NUM_MESSAGES; i++) {
      String message = String.format("This is message #%d", i);
      PostMessageRequest req = PostMessageRequest.newBuilder().setId(i).setMessage(message).build();
      stub.postMessage(req);
      System.out.println("Invoke method messages : " + message);
    }

    Map<Integer, String> messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
    assertEquals(NUM_MESSAGES, messages.size());

    stub.deleteMessage(DeleteMessageRequest.newBuilder().setId(1).build());
    messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
    assertEquals(NUM_MESSAGES - 1, messages.size());

    stub.postMessage(PostMessageRequest.newBuilder().setId(2).setMessage("updated message").build());
    messages = stub.getMessages(GetMessagesRequest.newBuilder().build()).getMessagesMap();
    assertEquals("updated message", messages.get(2));
  }

  @Test
  public void testInvokeTimeout() throws Exception {
    try (DaprClient resilientClient = new DaprClientBuilderFactory().newBuilder(DAPR_CONTAINER)
        .withResiliencyOptions(RESILIENCY_OPTIONS)
        .build()) {
      resilientClient.waitForSidecar(10000).block();
      MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub = createGrpcStub(resilientClient);
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
  public void testInvokeException() {
    MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub stub = createGrpcStub(daprClient);
    SleepRequest req = SleepRequest.newBuilder().setSeconds(-9).build();
    StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.sleep(req));

    assertEquals(Status.UNKNOWN.getCode(), exception.getStatus().getCode());
    assertEquals("Application error processing RPC", exception.getStatus().getDescription());
  }

  private MethodInvokeServiceGrpc.MethodInvokeServiceBlockingStub createGrpcStub(DaprClient client) {
    return client.newGrpcStub(APP_ID, MethodInvokeServiceGrpc::newBlockingStub);
  }

  private static class DaprClientBuilderFactory {
    io.dapr.client.DaprClientBuilder newBuilder(DaprContainer daprContainer) {
      return new io.dapr.client.DaprClientBuilder()
          .withPropertyOverride(io.dapr.config.Properties.HTTP_ENDPOINT, "http://localhost:" + daprContainer.getHttpPort())
          .withPropertyOverride(io.dapr.config.Properties.GRPC_ENDPOINT, "http://localhost:" + daprContainer.getGrpcPort());
    }
  }
}
