package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrchestrationInputTest {

    @Test
    void shouldStoreAllFields() {
        OrchestrationInput input = new OrchestrationInput("planner-1", 3, 10, true);

        assertThat(input.plannerId()).isEqualTo("planner-1");
        assertThat(input.agentCount()).isEqualTo(3);
        assertThat(input.maxIterations()).isEqualTo(10);
        assertThat(input.testExitAtLoopEnd()).isTrue();
    }

    @Test
    void shouldSupportEquality() {
        OrchestrationInput a = new OrchestrationInput("id", 2, 5, false);
        OrchestrationInput b = new OrchestrationInput("id", 2, 5, false);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldDetectInequality() {
        OrchestrationInput a = new OrchestrationInput("id1", 2, 5, false);
        OrchestrationInput b = new OrchestrationInput("id2", 2, 5, false);

        assertThat(a).isNotEqualTo(b);
    }
}
