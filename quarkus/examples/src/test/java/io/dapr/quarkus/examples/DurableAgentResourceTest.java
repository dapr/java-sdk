package io.dapr.quarkus.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Milestone-1 proof for the control-inversion approach: the agent's ReAct loop runs as a
 * durable Dapr Workflow ({@code react-agent}), driven by {@link DurableAgentResource}, with
 * the LLM call executed via the {@code agent-llm} activity.
 * <p>
 * Uses {@link MockChatModel} (no tool calls), so the workflow makes one {@code agent-llm}
 * call and completes. Requires Docker for Dapr dev services.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class DurableAgentResourceTest {

    @Test
    void durableAgentCompletesViaWorkflow() {
        given()
                .queryParam("topic", "dragons")
                .when()
                .get("/durable")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body(not(""));
    }
}
