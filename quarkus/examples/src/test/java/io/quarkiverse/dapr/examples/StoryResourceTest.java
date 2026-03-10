package io.quarkiverse.dapr.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration test for the story creation workflow.
 * <p>
 * Requires Docker for Dapr dev services (starts daprd, placement, scheduler,
 * PostgreSQL state store, and dashboard containers via Testcontainers).
 * Uses {@link MockChatModel} instead of a real LLM.
 */
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
