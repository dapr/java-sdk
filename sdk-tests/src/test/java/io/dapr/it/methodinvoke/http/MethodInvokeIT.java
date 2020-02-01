package io.dapr.it.methodinvoke.http;

import com.github.javafaker.Faker;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MethodInvokeIT extends BaseIT {

    //Number of messages to be sent: 10
    private static final int NUM_MESSAGES = 10;
    private static DaprRun daprRun=null;

    @BeforeClass
    public static void init() throws Exception {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        daprRun = startDaprApp(
                MethodInvokeIT.class.getSimpleName(),
                MethodInvokeService.SUCCESS_MESSAGE,
                MethodInvokeService.class,
                true,
                60000);
    }

    @Test
    public void testInvoke() {

        // At this point, it is guaranteed that the service above is running and all ports being listened to.

        DaprClient client = new DaprClientBuilder().build();
        for (int i = 0; i < NUM_MESSAGES; i++) {
            String message = String.format("This is message #%d", i);
            //Publishing messages
            client.invokeService(Verb.POST, daprRun.getAppName(), "messages", message).block();
            System.out.println("Invoke method messages : " + message);
        }

        Map<Integer,String> messages = client.invokeService(Verb.GET, daprRun.getAppName(), "messages", null, Map.class).block();
        assertEquals(10, messages.size());

        client.invokeService(Verb.DELETE,daprRun.getAppName(),"messages/1",null).block();

        messages = client.invokeService(Verb.GET, daprRun.getAppName(), "messages", null, Map.class).block();
        assertEquals(9, messages.size());

        client.invokeService(Verb.PUT, daprRun.getAppName(), "messages/2", "updated message").block();
        messages = client.invokeService(Verb.GET, daprRun.getAppName(), "messages", null, Map.class).block();
        assertEquals("updated message", messages.get("2"));

    }


    @Test
    public void testInvokeWithObjects()  {

        // At this point, it is guaranteed that the service above is running and all ports being listened to.

        DaprClient client = new DaprClientBuilder().build();

        Faker faker = new Faker();

        for (int i = 0; i < NUM_MESSAGES; i++) {
            Person person= new Person();
            person.setName(faker.name().name());
            person.setLastName(faker.name().lastName());
            person.setBirthDate(faker.date().birthday());
            //Publishing messages
            client.invokeService(Verb.POST, daprRun.getAppName(), "persons", person).block();
            System.out.println("Invoke method persons with parameter : " + person);
        }

        List<Person> persons = Arrays.asList(client.invokeService(Verb.GET, daprRun.getAppName(), "persons", null, Person[].class).block());
        assertEquals(10, persons.size());

        client.invokeService(Verb.DELETE,daprRun.getAppName(),"persons/1",null).block();

        persons = Arrays.asList(client.invokeService(Verb.GET, daprRun.getAppName(), "persons", null, Person[].class).block());
        assertEquals(9, persons.size());

        Person person= new Person();
        person.setName("John");
        person.setLastName("Smith");
        person.setBirthDate(Calendar.getInstance().getTime());

        client.invokeService(Verb.PUT, daprRun.getAppName(), "persons/2", person).block();

        persons = Arrays.asList(client.invokeService(Verb.GET, daprRun.getAppName(), "persons", null, Person[].class).block());
        Person resultPerson= persons.get(1);
        assertEquals("John", resultPerson.getName());
        assertEquals("Smith", resultPerson.getLastName());

    }

}
