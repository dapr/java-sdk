package io.dapr.quarkus.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

/**
 * HTTP smoke test for the {@code /loop} endpoint.
 * <p>
 * {@link LoopWriter} ({@code @LoopAgent}) runs {@link CreativeWriter} and {@link StyleEditor}
 * as a {@code durable-loop} workflow; a non-empty 200 response means the loop ran end-to-end.
 * The durable-loop semantics are covered in {@link DurableEntryPointTest}.
 * <p>
 * Requires Docker for Dapr dev services. Uses {@link MockChatModel} instead of a real LLM.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class LoopResourceTest {

    /**
     * Bound the HTTP wait: if the loop stalls, the request would otherwise hang forever.
     */
    private static final RestAssuredConfig BOUNDED = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.socket.timeout", 120_000));

    @Test
    void testLoopEndpointReturnsResponse() {
        given()
                .config(BOUNDED)
                .queryParam("topic", "dragons")
                .queryParam("style", "comedy")
                .when()
                .get("/loop")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(not(""));
    }
}
