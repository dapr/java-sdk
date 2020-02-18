/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PubSubIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;
    
    //The title of the topic to be used for publishing
    private static final String TOPIC_NAME = "testingtopic";

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

        DaprClient client = new DaprClientBuilder().build();
        for (int i = 0; i < NUM_MESSAGES; i++) {
            String message = String.format("This is message #%d", i);
            //Publishing messages
            client.publishEvent(TOPIC_NAME, message).block();
            System.out.println("Published message: " + message);
        }

        //Publishing a single byte: Example of non-string based content published
        client.publishEvent(
                TOPIC_NAME,
                new byte[] { 1 },
                Collections.singletonMap("content-type", "application/octet-stream")).block();
        System.out.println("Published one byte.");

        Thread.sleep(3000);

        callWithRetry(() -> {
            System.out.println("Checking results ...");
            final List<String> messages = client.invokeService(Verb.GET, daprRun.getAppName(), "messages", null, List.class).block();
            assertEquals(11, messages.size());

            for (int i = 0; i < NUM_MESSAGES; i++) {
                    assertTrue(messages.contains(String.format("This is message #%d", i)));
            }

            boolean foundByte = false;
            for (String message : messages) {
                if ((message.getBytes().length == 1) && (message.getBytes()[0] == 1)) {
                    foundByte = true;
                }
            }
            assertTrue(foundByte);

        }, 2000);
    }

}
