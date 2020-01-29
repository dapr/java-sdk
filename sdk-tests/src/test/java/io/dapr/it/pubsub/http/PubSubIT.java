package io.dapr.it.pubsub.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PubSubIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;
    //The title of the topic to be used for publishing
    private static final String TOPIC_NAME = "testingtopic";

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


        DaprClient client = new DaprClientBuilder(new DefaultObjectSerializer(), new DefaultObjectSerializer()).build();
        for (int i = 0; i < NUM_MESSAGES; i++) {
            String message = String.format("This is message #%d", i);
            //Publishing messages
            client.publishEvent(TOPIC_NAME, message).block();
            System.out.println("Published message: " + message);

            try {
                Thread.sleep((long)(1000 * Math.random()));
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }

        //Publishing a single bite: Example of non-string based content published
        client.publishEvent(
                TOPIC_NAME,
                new byte[] { 1 },
                Collections.singletonMap("content-type", "application/octet-stream")).block();
        System.out.println("Published one byte.");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        callWithRetry(() -> {
            System.out.println("Checking results ...");
            final List<String> messages = client.invokeService(Verb.GET, daprRun.getAppName(), "messages", null, List.class).block();
            assertEquals(11, messages.size());

            for (int i = 0; i < NUM_MESSAGES; i++) {

                    assertTrue( messages.get(i).startsWith("This is message "));

            }
            byte[] result=new byte[] { 1 };
            assertEquals( result.length,messages.get(10).getBytes().length);
            assertEquals(result[0],messages.get(10).getBytes()[0]);

        }, 2000);
    }

}
