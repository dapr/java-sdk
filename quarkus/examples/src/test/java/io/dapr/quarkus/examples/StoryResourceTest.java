package io.dapr.quarkus.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration test for the story creation workflow.
 * <p>
 * Requires Docker for Dapr dev services (starts daprd, placement, scheduler,
 * PostgreSQL state store, and dashboard containers via Testcontainers).
 * Uses {@link MockChatModel} instead of a real LLM.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
        disabledReason = "daprd 1.18.0 save-before-dispatch race (dapr/dapr#10054) loses workflow events; hangs are frequent on slow CI runners. Re-enable when the fixed runtime ships.")
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class StoryResourceTest {

    @Test
    void testStoryEndpointReturnsResponse() {
        given()
                .queryParam("topic", "dragons")
                .queryParam("style", "comedy")
                .when()
                .get("/story")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void testStoryEndpointWithDefaultParams() {
        given()
                .when()
                .get("/story")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    void testStoryEndpointResponseContainsContent() {
        String body = given()
                .queryParam("topic", "space exploration")
                .queryParam("style", "sci-fi")
                .when()
                .get("/story")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // The mock model always returns the same text; verify it's non-empty
        assert !body.isBlank() : "Story response should not be blank";
    }
}
