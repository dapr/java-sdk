package io.dapr.it.methodinvoke.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MethodInvokeIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;

    /**
     * Parameters for this test.
     * Param #1: useGrpc.
     * @return Collection of parameter tuples.
     */
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { false }, { true } });
    }

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    @Parameter
    public boolean useGrpc;

    @Before
    public void init() throws Exception {
        daprRun = startDaprApp(
          MethodInvokeIT.class.getSimpleName(),
          MethodInvokeService.SUCCESS_MESSAGE,
          MethodInvokeService.class,
          true,
          60000);

        if (this.useGrpc) {
            daprRun.switchToGRPC();
        } else {
            daprRun.switchToHTTP();
        }

        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }

    @Test
    public void testInvoke() throws IOException {

        // At this point, it is guaranteed that the service above is running and all ports being listened to.

        try (DaprClient client = new DaprClientBuilder().build()) {
            for (int i = 0; i < NUM_MESSAGES; i++) {
                String message = String.format("This is message #%d", i);
                //Publishing messages
                client.invokeService(daprRun.getAppName(), "messages", message.getBytes(), HttpExtension.POST).block();
                System.out.println("Invoke method messages : " + message);
            }

            Map<Integer, String> messages = client.invokeService(daprRun.getAppName(), "messages", null,
                HttpExtension.GET, Map.class).block();
            assertEquals(10, messages.size());

            client.invokeService(daprRun.getAppName(), "messages/1", null, HttpExtension.DELETE).block();

            messages = client.invokeService(daprRun.getAppName(), "messages", null, HttpExtension.GET, Map.class).block();
            assertEquals(9, messages.size());

            client.invokeService(daprRun.getAppName(), "messages/2", "updated message".getBytes(), HttpExtension.PUT).block();
            messages = client.invokeService(daprRun.getAppName(), "messages", null, HttpExtension.GET, Map.class).block();
            assertEquals("updated message", messages.get("2"));
        }
    }

    @Test
    public void testInvokeWithObjects() throws IOException {
        try (DaprClient client = new DaprClientBuilder().build()) {
            for (int i = 0; i < NUM_MESSAGES; i++) {
                Person person = new Person();
                person.setName(String.format("Name %d", i));
                person.setLastName(String.format("Last Name %d", i));
                person.setBirthDate(new Date());
                //Publishing messages
                client.invokeService(daprRun.getAppName(), "persons", person, HttpExtension.POST).block();
                System.out.println("Invoke method persons with parameter : " + person);
            }

            List<Person> persons = Arrays.asList(client.invokeService(daprRun.getAppName(), "persons", null, HttpExtension.GET, Person[].class).block());
            assertEquals(10, persons.size());

            client.invokeService(daprRun.getAppName(), "persons/1", null, HttpExtension.DELETE).block();

            persons = Arrays.asList(client.invokeService(daprRun.getAppName(), "persons", null, HttpExtension.GET, Person[].class).block());
            assertEquals(9, persons.size());

            Person person = new Person();
            person.setName("John");
            person.setLastName("Smith");
            person.setBirthDate(Calendar.getInstance().getTime());

            client.invokeService(daprRun.getAppName(), "persons/2", person, HttpExtension.PUT).block();

            persons = Arrays.asList(client.invokeService(daprRun.getAppName(), "persons", null, HttpExtension.GET, Person[].class).block());
            Person resultPerson = persons.get(1);
            assertEquals("John", resultPerson.getName());
            assertEquals("Smith", resultPerson.getLastName());
        }
    }
}
