package io.quarkiverse.dapr.langchain4j.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DaprAgentServiceUtilTest {

    @Test
    void shouldSanitizeName() {
        assertThat(DaprAgentServiceUtil.safeName("hello-world_123")).isEqualTo("hello-world_123");
    }

    @Test
    void shouldReplaceSpecialCharacters() {
        assertThat(DaprAgentServiceUtil.safeName("my agent!@#$%")).isEqualTo("my_agent_____");
    }

    @Test
    void shouldReplaceSpaces() {
        assertThat(DaprAgentServiceUtil.safeName("agent name with spaces")).isEqualTo("agent_name_with_spaces");
    }

    @Test
    void shouldHandleNullName() {
        assertThat(DaprAgentServiceUtil.safeName(null)).isEqualTo("unnamed");
    }

    @Test
    void shouldHandleEmptyName() {
        assertThat(DaprAgentServiceUtil.safeName("")).isEqualTo("unnamed");
    }

    @Test
    void shouldPreserveAlphanumericHyphenUnderscore() {
        assertThat(DaprAgentServiceUtil.safeName("ABC-def_123")).isEqualTo("ABC-def_123");
    }
}
