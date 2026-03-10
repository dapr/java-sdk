package io.quarkiverse.dapr.examples;

import java.io.File;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 {@link ExecutionCondition} that disables tests when Docker is not available.
 * This prevents Dapr Testcontainers-based integration tests from failing in
 * environments without Docker (e.g., CI without Docker-in-Docker).
 * <p>
 * Checks for Docker by looking for the Docker socket file or running
 * {@code docker info}. Usage: annotate test classes with
 * {@code @ExtendWith(DockerAvailableCondition.class)}.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        // Check common Docker socket locations
        if (new File("/var/run/docker.sock").exists()) {
            return ConditionEvaluationResult.enabled("Docker socket found at /var/run/docker.sock");
        }

        // Try Docker Desktop on macOS
        String home = System.getProperty("user.home");
        if (home != null && new File(home + "/.docker/run/docker.sock").exists()) {
            return ConditionEvaluationResult.enabled("Docker socket found at ~/.docker/run/docker.sock");
        }

        // Fallback: try running 'docker info'
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return ConditionEvaluationResult.enabled("Docker is available (docker info succeeded)");
            }
        } catch (Exception e) {
            // docker command not found or failed
        }

        return ConditionEvaluationResult.disabled("Docker is not available, skipping integration test");
    }
}
