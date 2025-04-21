package io.dapr.it.methodinvoke.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class GH_1314_IT {

  private static WireMockServer wireMockServer;

  @BeforeAll
  static void setup() {
    wireMockServer = new WireMockServer(3500);

    wireMockServer.start();

    WireMock.configureFor("localhost", 3500);

    stubFor(post(
        urlEqualTo("/v1.0/invoke/say-hello/method/say-hello/hello")
    )
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("\"Hello from say-hello app!\"")));
  }

  @AfterAll
  static void teardown() {
    wireMockServer.stop();
  }

  @Test
  void testInvokeSayHelloWithoutLeadingSlash() {
    String urlWithoutLeadingSlash = "say-hello/hello";

    sayHelloUsingURL(urlWithoutLeadingSlash);
  }

  @Test
  void testInvokeSayHelloWithLeadingSlash() {
    String urlWithLeadingSlash = "/say-hello/hello";

    sayHelloUsingURL(urlWithLeadingSlash);
  }

  private static void sayHelloUsingURL(String url) {
    DaprClient client = new DaprClientBuilder().build();

    try {
      Map<String, String> requestData = Map.of("message", "Hello");

      String response = client.invokeMethod(
          "say-hello",
          url,
          requestData,
          HttpExtension.POST,
          String.class
      ).block();

      assertEquals("Hello from say-hello app!", response);
    } catch (Exception e) {
      fail("Exception occurred: " + e.getMessage());
    } finally {
      try {
        client.close();
      } catch (Exception e) {
        fail(e);
      }
    }
  }

}
