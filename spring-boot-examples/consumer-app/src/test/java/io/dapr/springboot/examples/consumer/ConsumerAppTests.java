package io.dapr.springboot.examples.consumer;

import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.springboot.DaprAutoConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;


@SpringBootTest(classes = {TestConsumerApplication.class, DaprTestContainersConfig.class,
        ConsumerAppTestConfiguration.class, DaprAutoConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ConsumerAppTests {

  @Autowired
  private DaprMessagingTemplate<Order> messagingTemplate;

  @Autowired
  private SubscriberRestController subscriberRestController;

  @BeforeAll
  public static void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(8081);
  }

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8081;
  }


  @Test
  void testMessageConsumer() throws InterruptedException, IOException {

    messagingTemplate.send("topic", new Order("abc-123", "the mars volta LP", 1));


    given()
            .contentType(ContentType.JSON)
            .when()
            .get("/events")
            .then()
            .statusCode(200);


    await()
            .atMost(Duration.ofSeconds(5))
            .until(subscriberRestController.getAllEvents()::size, equalTo(1));


  }

}
