package io.quarkiverse.dapr.langchain4j.workflow;

import io.dapr.workflows.Workflow;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;

/**
 * Resolves the registration name for a {@link Workflow} class.
 * If the class is annotated with {@link WorkflowMetadata} and provides a non-empty
 * {@code name}, that name is returned; otherwise, the fully-qualified class name is used.
 */
public final class WorkflowNameResolver {

    private WorkflowNameResolver() {
    }

    /**
     * Returns the Dapr registration name for the given workflow class.
     */
    public static String resolve(Class<? extends Workflow> workflowClass) {
        WorkflowMetadata meta = workflowClass.getAnnotation(WorkflowMetadata.class);
        if (meta != null && !meta.name().isEmpty()) {
            return meta.name();
        }
        return workflowClass.getCanonicalName();
    }
}
