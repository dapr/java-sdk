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

package io.dapr.it.configuration;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigurationClientIT extends BaseIT {

    private static final String CONFIG_STORE_NAME = "redisconfigstore";

    private static DaprRun daprRun;

    private static DaprClient daprClient;

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

    @BeforeAll
    public static void init() throws Exception {
        daprRun = startDaprApp(ConfigurationClientIT.class.getSimpleName(), 5000);
        daprClient = new DaprClientBuilder().build();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        daprClient.close();
    }

    @BeforeEach
    public void setupConfigStore() {
        executeDockerCommand(insertCmd);
    }

    @Test
    public void getConfiguration() {
        ConfigurationItem ci = daprClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1").block();
        assertEquals(ci.getValue(), "myconfigvalue1");
    }

    @Test
    public void getConfigurations() {
        Map<String, ConfigurationItem> cis = daprClient.getConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2").block();
        assertTrue(cis.size() == 2);
        assertTrue(cis.containsKey("myconfigkey1"));
        assertTrue(cis.containsKey("myconfigkey2"));
        assertEquals(cis.get("myconfigkey2").getValue(), "myconfigvalue2");
    }

    @Test
    public void subscribeConfiguration() {
        Runnable subscribeTask = () -> {
            Flux<SubscribeConfigurationResponse> outFlux = daprClient
                    .subscribeConfiguration(CONFIG_STORE_NAME, "myconfigkey1", "myconfigkey2");
            outFlux.subscribe(update -> {
                if (update.getItems().size() == 0 ) {
                    assertTrue(update.getSubscriptionId().length() > 0);
                } else {
                    String value = update.getItems().entrySet().stream().findFirst().get().getValue().getValue();
                    assertEquals(update.getItems().size(), 1);
                    assertTrue(value.contains("update_"));
                }
            });
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
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void unsubscribeConfigurationItems() {
        List<String> updatedValues = new ArrayList<>();
        AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>();
        AtomicReference<String> subscriptionId = new AtomicReference<>();
        Runnable subscribeTask = () -> {
            Flux<SubscribeConfigurationResponse> outFlux = daprClient
                    .subscribeConfiguration(CONFIG_STORE_NAME, "myconfigkey1");
            disposableAtomicReference.set(outFlux
                .subscribe(update -> {
                        subscriptionId.set(update.getSubscriptionId());
                        updatedValues.add(update.getItems().entrySet().stream().findFirst().get().getValue().getValue());
                    }
                ));
        };
        new Thread(subscribeTask).start();

        // To ensure that subscribeThread gets scheduled
        inducingSleepTime(0);

        Runnable updateKeys = () -> {
            int i = 1;
            while (i <= 5) {
                String[] command = new String[] {
                        "docker", "exec", "dapr_redis", "redis-cli",
                        "SET",
                        "myconfigkey1", "update_myconfigvalue" + i + "||2"
                };
                executeDockerCommand(command);
                i++;
            }
        };
        new Thread(updateKeys).start();

        // To ensure key starts getting updated
        inducingSleepTime(1000);

        UnsubscribeConfigurationResponse res = daprClient.unsubscribeConfiguration(
            subscriptionId.get(),
            CONFIG_STORE_NAME
        ).block();

        assertTrue(res != null);
        assertTrue(res.getIsUnsubscribed());
        int listSize = updatedValues.size();
        // To ensure main thread does not die
        inducingSleepTime(1000);

        new Thread(updateKeys).start();

        // To ensure main thread does not die
        inducingSleepTime(2000);
        assertTrue(updatedValues.size() == listSize);
    }

    private static void inducingSleepTime(int timeInMillis) {
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void executeDockerCommand(String[] command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = null;
        try {
            process = processBuilder.start();
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("Not zero exit code for Redis command: " + process.exitValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
