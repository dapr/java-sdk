/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.configuration.grpc;

import io.dapr.client.DaprPreviewClient;
import io.dapr.client.DaprPreviewClientBuilder;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.*;

import static org.junit.Assert.*;

public class ConfigurationClientIT extends BaseIT {

    private static final String CONFIG_STORE_NAME = "redisconfigstore";

    private static DaprRun daprRun;

    private static DaprPreviewClient daprPreviewClient;

    private static String key = "myconfig1";

    private static List<String> keys = new ArrayList<>(Arrays.asList("myconfig1", "myconfig2", "myconfig3"));

    @BeforeClass
    public static void init() throws Exception {
        /*  setup redis configuration store to have below key-value pairs before running the tests.
            this step is required as there is no save api for configuration data.
            config store name -> redisconfigstore
            below 3 key-value pairs -
                myconfigkey1-myconfigvalue1
                myconfigkey2-myconfigvalue2
                myconfigkey3-myconfigvalue3
        */
        daprRun = startDaprApp(ConfigurationClientIT.class.getSimpleName(), 5000);
        daprRun.switchToGRPC();
        daprPreviewClient = new DaprPreviewClientBuilder().build();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        daprPreviewClient.close();
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
        List<ConfigurationItem> cis = daprPreviewClient.getConfigurations(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2").block();
        assertTrue(cis.size() == 2);
        assertEquals(cis.get(0).getKey(), "myconfigkey1");
        assertEquals(cis.get(1).getValue(), "myconfigvalue2");
    }

    @Test
    public void getConfigurationsWithEmptyList() {
        List<String> listOfKeys = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            daprPreviewClient.getConfigurations(CONFIG_STORE_NAME, listOfKeys, metadata).block();
        });
    }

    @Test
    public void subscribeToConfiguration() {
        Flux<List<ConfigurationItem>> outFlux = daprPreviewClient.subscribeToConfigurations(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2");
        assertNotNull(outFlux);
    }
}
