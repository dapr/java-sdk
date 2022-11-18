package io.dapr.it.configuration.http;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class ConfigurationIT extends BaseIT {
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
        daprRun = startDaprApp(ConfigurationIT.class.getSimpleName(), 5000);
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
    public void getConfigurations() {
        Map<String, ConfigurationItem> cis = daprPreviewClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2").block();
        assertTrue(cis.size() == 2);
        assertTrue(cis.containsKey("myconfigkey1"));
        assertTrue(cis.containsKey("myconfigkey2"));
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
