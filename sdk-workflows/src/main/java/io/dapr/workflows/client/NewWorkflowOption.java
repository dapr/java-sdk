package io.dapr.workflows.client;

import com.microsoft.durabletask.NewOrchestrationInstanceOptions;
import java.time.Instant;

/**
 * Options for starting a new instance of a workflow.
 */
public class NewWorkflowOption {
    private final NewOrchestrationInstanceOptions newOrchestrationInstanceOptions = new NewOrchestrationInstanceOptions();

    /**
     * Sets the version of the workflow to start.
     *
     * @param version the user-defined version of workflow
     * @return this {@link NewWorkflowOption} object
     */
    public NewWorkflowOption setVersion(String version) {
        this.newOrchestrationInstanceOptions.setVersion(version);
        return this;
    }

    /**
     * Sets the instance ID of the workflow to start.
     * <p>
     * If no instance ID is configured, the workflow will be created with a randomly generated instance ID.
     *
     * @param instanceId the ID of the new workflow
     * @return this {@link NewWorkflowOption} object
     */
    public NewWorkflowOption setInstanceId(String instanceId) {
        this.newOrchestrationInstanceOptions.setInstanceId(instanceId);
        return this;
    }

    /**
     * Sets the input of the workflow to start.
     *
     * @param input the input of the new workflow
     * @return this {@link NewWorkflowOption} object
     */
    public NewWorkflowOption setInput(Object input) {
        this.newOrchestrationInstanceOptions.setInput(input);
        return this;
    }

    /**
     * Sets the start time of the new workflow.
     * <p>
     * By default, new workflow instances start executing immediately. This method can be used
     * to start them at a specific time in the future.
     *
     * @param startTime the start time of the new workflow
     * @return this {@link NewWorkflowOption} object
     */
    public NewWorkflowOption setStartTime(Instant startTime) {
        this.newOrchestrationInstanceOptions.setStartTime(startTime);
        return this;
    }

    /**
     * Gets the user-specified version of the new workflow.
     *
     * @return the user-specified version of the new workflow.
     */
    public String getVersion() {
        return this.newOrchestrationInstanceOptions.getVersion();
    }

    /**
     * Gets the instance ID of the new workflow.
     *
     * @return the instance ID of the new workflow.
     */
    public String getInstanceId() {
        return this.newOrchestrationInstanceOptions.getInstanceId();
    }

    /**
     * Gets the input of the new workflow.
     *
     * @return the input of the new workflow.
     */
    public Object getInput() {
        return this.newOrchestrationInstanceOptions.getInput();
    }

    /**
     * Gets the configured start time of the new workflow instance.
     *
     * @return the configured start time of the new workflow instance.
     */
    public Instant getStartTime() {
        return this.newOrchestrationInstanceOptions.getStartTime();
    }

    public NewOrchestrationInstanceOptions getNewOrchestrationInstanceOptions() {
        return newOrchestrationInstanceOptions;
    }
}
