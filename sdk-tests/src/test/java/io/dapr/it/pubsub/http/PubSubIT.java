/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprHttp;
import io.dapr.client.HttpExtension;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PubSubIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;
    
    //The title of the topic to be used for publishing
    private static final String TOPIC_NAME = "testingtopic";
    private static final String ANOTHER_TOPIC_NAME = "anothertopic";

    /**
     * Parameters for this test.
     * Param #1: useGrpc.
     * @return Collection of parameter tuples.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }

    @Parameterized.Parameter
    public boolean useGrpc;

    @Test
    public void testPubSub() throws Exception {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        final DaprRun daprRun = startDaprApp(
                this.getClass().getSimpleName(),
                SubscriberService.SUCCESS_MESSAGE,
                SubscriberService.class,
                true,
                60000);
        // At this point, it is guaranteed that the service above is running and all ports being listened to.
        if (this.useGrpc) {
            daprRun.switchToGRPC();
        } else {
            daprRun.switchToHTTP();
        }

        // Send a batch of messages on one topic
        DaprClient client = new DaprClientBuilder().build();
        for (int i = 0; i < NUM_MESSAGES; i++) {
            String message = String.format("This is message #%d on topic %s", i, TOPIC_NAME);
            //Publishing messages
            client.publishEvent(TOPIC_NAME, message).block();
            System.out.println(String.format("Published message: '%s' to topic '%s'", message, TOPIC_NAME));
        }

        // Send a batch of different messages on the other.
        for (int i = 0; i < NUM_MESSAGES; i++) {
            String message = String.format("This is message #%d on topic %s", i, ANOTHER_TOPIC_NAME);
            //Publishing messages
            client.publishEvent(ANOTHER_TOPIC_NAME, message).block();
            System.out.println(String.format("Published message: '%s' to topic '%s'", message, ANOTHER_TOPIC_NAME));
        }

        //Publishing a single byte: Example of non-string based content published
        client.publishEvent(
                TOPIC_NAME,
                new byte[] { 1 },
                Collections.singletonMap("content-type", "application/octet-stream")).block();
        System.out.println("Published one byte.");

        Thread.sleep(3000);

        callWithRetry(() -> {
            System.out.println("Checking results for topic " + TOPIC_NAME);
            final List<String> messages = client.invokeService(daprRun.getAppName(), "messages/testingtopic", null, HttpExtension.GET, List.class).block();
            assertEquals(11, messages.size());

            for (int i = 0; i < NUM_MESSAGES; i++) {
                assertTrue(messages.contains(String.format("This is message #%d on topic %s", i, TOPIC_NAME)));
            }

            boolean foundByte = false;
            for (String message : messages) {
                if ((message.getBytes().length == 1) && (message.getBytes()[0] == 1)) {
                    foundByte = true;
                }
            }
            assertTrue(foundByte);

        }, 2000);

        callWithRetry(() -> {
            System.out.println("Checking results for topic " + ANOTHER_TOPIC_NAME);
            final List<String> messages = client.invokeService(daprRun.getAppName(), "messages/anothertopic", null, HttpExtension.GET, List.class).block();
            assertEquals(10, messages.size());

            for (int i = 0; i < NUM_MESSAGES; i++) {
                assertTrue(messages.contains(String.format("This is message #%d on topic %s", i, ANOTHER_TOPIC_NAME)));
            }
        }, 2000);
    }

}
