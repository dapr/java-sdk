package io.dapr.it.methodinvoke.http;

import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprHttp;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaprSpringBootTest(classes = MethodInvokeService.class)
@Tag("testcontainers")
public class MethodInvokeIT {

  private static final String APP_ID = "methodinvoke-http";

  // Number of messages to be sent: 10
  private static final int NUM_MESSAGES = 10;

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest(APP_ID);

  private DaprClient daprClient;

  @BeforeEach
  public void init() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    daprClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
    daprClient.waitForSidecar(10000).block();
  }

  @AfterEach
  public void closeClient() throws Exception {
    daprClient.close();
  }

  @Test
  public void testInvoke() {
    for (int i = 0; i < NUM_MESSAGES; i++) {
      String message = String.format("This is message #%d", i);
      daprClient.invokeMethod(APP_ID, "messages", message.getBytes(), HttpExtension.POST).block();
      System.out.println("Invoke method messages : " + message);
    }

    Map<Integer, String> messages = daprClient.invokeMethod(APP_ID, "messages", null,
        HttpExtension.GET, Map.class).block();
    assertEquals(10, messages.size());

    daprClient.invokeMethod(APP_ID, "messages/1", null, HttpExtension.DELETE).block();

    messages = daprClient.invokeMethod(APP_ID, "messages", null, HttpExtension.GET, Map.class).block();
    assertEquals(9, messages.size());

    daprClient.invokeMethod(APP_ID, "messages/2", "updated message".getBytes(), HttpExtension.PUT).block();
    messages = daprClient.invokeMethod(APP_ID, "messages", null, HttpExtension.GET, Map.class).block();
    assertEquals("updated message", messages.get("2"));
  }

  @Test
  public void testInvokeWithObjects() {
    for (int i = 0; i < NUM_MESSAGES; i++) {
      Person person = new Person();
      person.setName(String.format("Name %d", i));
      person.setLastName(String.format("Last Name %d", i));
      person.setBirthDate(new Date());
      daprClient.invokeMethod(APP_ID, "persons", person, HttpExtension.POST).block();
      System.out.println("Invoke method persons with parameter : " + person);
    }

    List<Person> persons = Arrays.asList(daprClient.invokeMethod(APP_ID, "persons", null, HttpExtension.GET,
        Person[].class).block());
    assertEquals(10, persons.size());

    daprClient.invokeMethod(APP_ID, "persons/1", null, HttpExtension.DELETE).block();

    persons = Arrays.asList(daprClient.invokeMethod(APP_ID, "persons", null, HttpExtension.GET, Person[].class).block());
    assertEquals(9, persons.size());

    Person person = new Person();
    person.setName("John");
    person.setLastName("Smith");
    person.setBirthDate(Calendar.getInstance().getTime());

    daprClient.invokeMethod(APP_ID, "persons/2", person, HttpExtension.PUT).block();

    persons = Arrays.asList(daprClient.invokeMethod(APP_ID, "persons", null, HttpExtension.GET, Person[].class).block());
    Person resultPerson = persons.get(1);
    assertEquals("John", resultPerson.getName());
    assertEquals("Smith", resultPerson.getLastName());
  }

  @Test
  public void testInvokeTimeout() {
    long started = System.currentTimeMillis();
    String message = assertThrows(IllegalStateException.class, () ->
        daprClient.invokeMethod(APP_ID, "sleep", 1, HttpExtension.POST).block(Duration.ofMillis(10))
    ).getMessage();

    long delay = System.currentTimeMillis() - started;
    assertTrue(delay <= 200, "Delay: " + delay + " is not less than timeout: 200");
    assertEquals("Timeout on blocking read for 10000000 NANOSECONDS", message);
  }

  @Test
  public void testInvokeException() {
    DaprException exception = assertThrows(DaprException.class, () ->
        daprClient.invokeMethod(APP_ID, "sleep", -9, HttpExtension.POST).block());

    // TODO(artursouza): change this to INTERNAL once runtime is fixed.
    assertEquals("UNKNOWN", exception.getErrorCode());
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains("HTTP status code: 500"));
    assertTrue(new String(exception.getPayload()).contains("Internal Server Error"));
  }

  @Test
  public void testInvokeQueryParamEncoding() {
    String uri = "abc/pqr";
    Map<String, List<String>> queryParams = Map.of("uri", List.of(uri));
    HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET, queryParams, Map.of());
    JsonNode result = daprClient.invokeMethod(
        APP_ID,
        "/query",
        null,
        httpExtension,
        JsonNode.class
    ).block();

    assertEquals(uri, result.get("uri").asText());
  }
}
