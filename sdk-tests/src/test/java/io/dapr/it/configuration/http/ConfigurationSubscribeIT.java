package io.dapr.it.configuration.http;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

public class ConfigurationSubscribeIT extends BaseIT {
  private static final String CONFIG_STORE_NAME = "redisconfigstore";

  private static DaprRun daprRun;

  private static DaprPreviewClient daprPreviewClient;

  private static String key = "myconfig1";

  private static List<String> keys = new ArrayList<>(Arrays.asList("myconfig1", "myconfig2", "myconfig3"));

  private static String[] insertCmd = new String[] {
      "docker", "exec", "dapr_redis", "redis-cli",
      "MSET",
      "myconfigkey1", "myconfigvalue1||1",
      "myconfigkey2", "myconfigvalue2||1",
      "myconfigkey3", "myconfigvalue3||1"
  };

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(
        ConfigurationIT.class.getSimpleName(),
        ConfigurationSubscriberService.SUCCESS_MESSAGE,
        ConfigurationSubscriberService.class,
        true,
        60000);
    daprRun.switchToHTTP();
    daprPreviewClient = new DaprClientBuilder().buildPreviewClient();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    daprPreviewClient.close();
  }

  @Before
  public void setupConfigStore() {
    executeDockerCommand(insertCmd);
  }

  @Test
  public void subscribeAndUnsubscribeConfiguration() {
    AtomicReference<String> subId= new AtomicReference<>("");
    Flux<SubscribeConfigurationResponse> outFlux = daprPreviewClient
        .subscribeConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2");
    outFlux.subscribe(items -> {
      subId.set(items.getSubscriptionId());
    });
    assertTrue(subId.get().length() > 0);

    UnsubscribeConfigurationResponse res = daprPreviewClient.unsubscribeConfiguration(
        subId.get(),
        CONFIG_STORE_NAME
    ).block();
    assertTrue(res.getIsUnsubscribed());
  }

  private static void executeDockerCommand(String[] command) {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    Process process = null;
    try {
      process = processBuilder.start();
      process.waitFor();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
