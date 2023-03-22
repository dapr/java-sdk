package io.dapr.it.methodinvoke.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.MethodInvokeServiceProtos;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MethodInvokeIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    @Before
    public void init() throws Exception {
        daprRun = startDaprApp(
          MethodInvokeIT.class.getSimpleName(),
          MethodInvokeService.SUCCESS_MESSAGE,
          MethodInvokeService.class,
          true,
          30000);
        daprRun.switchToHTTP();
        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }

    @Test
    public void testInvoke() throws Exception {

        // At this point, it is guaranteed that the service above is running and all ports being listened to.

        try (DaprClient client = new DaprClientBuilder().build()) {
            for (int i = 0; i < NUM_MESSAGES; i++) {
                String message = String.format("This is message #%d", i);
                //Publishing messages
                client.invokeMethod(daprRun.getAppName(), "messages", message.getBytes(), HttpExtension.POST).block();
                System.out.println("Invoke method messages : " + message);
            }

            Map<Integer, String> messages = client.invokeMethod(daprRun.getAppName(), "messages", null,
                HttpExtension.GET, Map.class).block();
            assertEquals(10, messages.size());

            client.invokeMethod(daprRun.getAppName(), "messages/1", null, HttpExtension.DELETE).block();

            messages = client.invokeMethod(daprRun.getAppName(), "messages", null, HttpExtension.GET, Map.class).block();
            assertEquals(9, messages.size());

            client.invokeMethod(daprRun.getAppName(), "messages/2", "updated message".getBytes(), HttpExtension.PUT).block();
            messages = client.invokeMethod(daprRun.getAppName(), "messages", null, HttpExtension.GET, Map.class).block();
            assertEquals("updated message", messages.get("2"));
        }
    }

    @Test
    public void testInvokeWithObjects() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            for (int i = 0; i < NUM_MESSAGES; i++) {
                Person person = new Person();
                person.setName(String.format("Name %d", i));
                person.setLastName(String.format("Last Name %d", i));
                person.setBirthDate(new Date());
                //Publishing messages
                client.invokeMethod(daprRun.getAppName(), "persons", person, HttpExtension.POST).block();
                System.out.println("Invoke method persons with parameter : " + person);
            }

            List<Person> persons = Arrays.asList(client.invokeMethod(daprRun.getAppName(), "persons", null, HttpExtension.GET, Person[].class).block());
            assertEquals(10, persons.size());

            client.invokeMethod(daprRun.getAppName(), "persons/1", null, HttpExtension.DELETE).block();

            persons = Arrays.asList(client.invokeMethod(daprRun.getAppName(), "persons", null, HttpExtension.GET, Person[].class).block());
            assertEquals(9, persons.size());

            Person person = new Person();
            person.setName("John");
            person.setLastName("Smith");
            person.setBirthDate(Calendar.getInstance().getTime());

            client.invokeMethod(daprRun.getAppName(), "persons/2", person, HttpExtension.PUT).block();

            persons = Arrays.asList(client.invokeMethod(daprRun.getAppName(), "persons", null, HttpExtension.GET, Person[].class).block());
            Person resultPerson = persons.get(1);
            assertEquals("John", resultPerson.getName());
            assertEquals("Smith", resultPerson.getLastName());
        }
    }

    @Test
    public void testInvokeTimeout() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            long started = System.currentTimeMillis();
            String message = assertThrows(IllegalStateException.class, () -> {
                client.invokeMethod(daprRun.getAppName(), "sleep", 1, HttpExtension.POST)
                    .block(Duration.ofMillis(10));
            }).getMessage();
            long delay = System.currentTimeMillis() - started;
            assertTrue(delay <= 200);  // 200 ms is a reasonable delay if the request timed out.
            assertEquals("Timeout on blocking read for 10000000 NANOSECONDS", message);
        }
    }

    @Test
    public void testInvokeException() throws Exception {
        try (DaprClient client = new DaprClientBuilder().build()) {
            MethodInvokeServiceProtos.SleepRequest req = MethodInvokeServiceProtos.SleepRequest.newBuilder().setSeconds(-9).build();
            DaprException exception = assertThrows(DaprException.class, () ->
                client.invokeMethod(daprRun.getAppName(), "sleep", -9, HttpExtension.POST).block());

            // TODO(artursouza): change this to INTERNAL once runtime is fixed.
            assertEquals("UNKNOWN", exception.getErrorCode());
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("Internal Server Error"));
        }
    }
}
