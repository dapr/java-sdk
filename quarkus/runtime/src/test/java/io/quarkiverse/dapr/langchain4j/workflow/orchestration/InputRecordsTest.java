package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InputRecordsTest {

    @Test
    void agentExecInputShouldStoreFields() {
        AgentExecInput input = new AgentExecInput("planner-abc", 2, "planner-abc:2");

        assertThat(input.plannerId()).isEqualTo("planner-abc");
        assertThat(input.agentIndex()).isEqualTo(2);
        assertThat(input.agentRunId()).isEqualTo("planner-abc:2");
    }

    @Test
    void agentExecInputShouldSupportEquality() {
        AgentExecInput a = new AgentExecInput("id", 1, "id:1");
        AgentExecInput b = new AgentExecInput("id", 1, "id:1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void conditionCheckInputShouldStoreFields() {
        ConditionCheckInput input = new ConditionCheckInput("planner-xyz", 5);

        assertThat(input.plannerId()).isEqualTo("planner-xyz");
        assertThat(input.agentIndex()).isEqualTo(5);
    }

    @Test
    void exitConditionCheckInputShouldStoreFields() {
        ExitConditionCheckInput input = new ExitConditionCheckInput("planner-loop", 7);

        assertThat(input.plannerId()).isEqualTo("planner-loop");
        assertThat(input.iteration()).isEqualTo(7);
    }

    @Test
    void differentRecordTypesShouldNotBeEqual() {
        AgentExecInput agent = new AgentExecInput("id", 1, "id:1");
        ConditionCheckInput condition = new ConditionCheckInput("id", 1);

        // They're different record types, so they shouldn't be equal
        assertThat(agent).isNotEqualTo(condition);
    }
}
