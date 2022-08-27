/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.it.configuration.grpc;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;

import org.junit.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

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
    public static void init() throws Exception {
        daprRun = startDaprApp(ConfigurationClientIT.class.getSimpleName(), 5000);
        daprRun.switchToGRPC();
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
        Map<String, ConfigurationItem> cis = daprPreviewClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2").block();
        assertTrue(cis.size() == 2);
        assertTrue(cis.containsKey("myconfigkey1"));
        assertTrue(cis.containsKey("myconfigkey2"));
        assertEquals(cis.get("myconfigkey2").getValue(), "myconfigvalue2");
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
    public void subscribeToConfiguration() {
        List<String> updatedValues = new ArrayList<>();
        AtomicReference<Disposable> disposable = new AtomicReference<>();
        Runnable subscribeTask = () -> {
            Flux<Map<String, ConfigurationItem>> outFlux = daprPreviewClient
                    .subscribeToConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2");
            disposable.set(outFlux.subscribe(update -> {
                updatedValues.add(update.get(0).getValue());
            }));
        };
        Thread subscribeThread = new Thread(subscribeTask);
        subscribeThread.start();
        try {
            // To ensure that subscribeThread gets scheduled
            Thread.sleep(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Runnable updateKeys = () -> {
            executeDockerCommand(updateCmd);
        };
        new Thread(updateKeys).start();
        try {
            // To ensure main thread does not die before outFlux subscribe gets called
            Thread.sleep(3000);
            disposable.get().dispose();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(updatedValues.size(), 2);
        assertTrue(updatedValues.contains("update_myconfigvalue1"));
        assertTrue(updatedValues.contains("update_myconfigvalue2"));
        assertFalse(updatedValues.contains("update_myconfigvalue3"));
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
