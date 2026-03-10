package io.quarkiverse.dapr.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration test for the parallel creation workflow.
 * <p>
 * {@link ParallelCreator} runs {@link StoryCreator} (a nested {@code @SequenceAgent})
 * and {@link ResearchWriter} in parallel, verifying that nested composite agents work
 * correctly with Dapr Workflows.
 * <p>
 * Requires Docker for Dapr dev services. Uses {@link MockChatModel} instead of a real LLM.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class ParallelResourceTest {

    @Test
    void testParallelEndpointReturnsResponse() {
        given()
                .queryParam("topic", "dragons")
                .queryParam("country", "France")
                .queryParam("style", "comedy")
                .when()
                .get("/parallel")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("story", notNullValue())
                .body("summary", notNullValue());
    }

    @Test
    void testParallelEndpointWithDefaultParams() {
        given()
                .when()
                .get("/parallel")
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"))
                .body("story", notNullValue())
                .body("summary", notNullValue());
    }
}