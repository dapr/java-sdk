package io.dapr.quarkus.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import io.dapr.quarkus.langchain4j.agent.AgentRunBindingRegistry;
import io.dapr.quarkus.langchain4j.agent.DaprAgentRunRegistry;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

/**
 * Integration test for the loop orchestration workflow.
 * <p>
 * {@link LoopWriter} runs {@link CreativeWriter} and {@link StyleEditor} twice in a loop,
 * so the same agents execute in both iterations. This verifies the per-iteration agent-run
 * bookkeeping: iteration 1 must run through the normal LLM/tool activity path (not crash
 * recovery) and leave no stale registry state behind.
 * <p>
 * Requires Docker for Dapr dev services. Uses {@link MockChatModel} instead of a real LLM.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
        disabledReason = "daprd 1.18.0 save-before-dispatch race (dapr/dapr#10054) loses workflow events; hangs are frequent on slow CI runners. Re-enable when the fixed runtime ships.")
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class LoopResourceTest {

    /**
     * Bound the HTTP wait: when iteration 1 of the loop stalls (e.g. its child agent-run
     * never completes), the request would otherwise hang forever.
     */
    private static final RestAssuredConfig BOUNDED = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.socket.timeout", 120_000));

    @Test
    void testLoopRunsBothIterationsThroughNormalPath() {
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

        // All four agent runs (2 iterations x 2 agents) completed: no in-flight run
        // contexts may remain registered.
        assertTrue(DaprAgentRunRegistry.getRegisteredIds().isEmpty(),
                "leftover AgentRunContexts: " + DaprAgentRunRegistry.getRegisteredIds());

        // Every bound agentRunId must have been claimed by its own iteration's run.
        // A leftover binding means a later iteration would claim a stale run id.
        assertNull(AgentRunBindingRegistry.claim("creative-writer-agent"),
                "stale binding left for creative-writer-agent");
        assertNull(AgentRunBindingRegistry.claim("style-editor-agent"),
                "stale binding left for style-editor-agent");
    }
}
