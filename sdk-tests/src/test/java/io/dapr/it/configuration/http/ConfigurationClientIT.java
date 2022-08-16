package io.dapr.it.configuration.http;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConfigurationItem;
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

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class ConfigurationClientIT extends BaseIT {
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

    private static String[] updateCmd = new String[] {
            "docker", "exec", "dapr_redis", "redis-cli",
            "MSET",
            "myconfigkey1", "update_myconfigvalue1||2",
            "myconfigkey2", "update_myconfigvalue2||2",
            "myconfigkey3", "update_myconfigvalue3||2"
    };

    @BeforeClass
    public void init() throws Exception {
        daprRun = startDaprApp(this.getClass().getSimpleName(), 5000);
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
    public void getConfiguration() {
        ConfigurationItem ci = daprPreviewClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1").block();
        assertEquals(ci.getKey(), "myconfigkey1");
        assertEquals(ci.getValue(), "myconfigvalue1");
    }

    @Test
    public void getConfigurationWithEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            daprPreviewClient.getConfiguration(CONFIG_STORE_NAME, "").block();
        });
    }

    @Test
    public void getConfigurations() {
        List<ConfigurationItem> cis = daprPreviewClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2").block();
        assertTrue(cis.size() == 2);
        assertEquals(cis.get(0).getKey(), "myconfigkey1");
        assertEquals(cis.get(1).getValue(), "myconfigvalue2");
    }

    @Test
    public void getConfigurationsWithEmptyList() {
        List<String> listOfKeys = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            daprPreviewClient.getConfiguration(CONFIG_STORE_NAME, listOfKeys, metadata).block();
        });
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
