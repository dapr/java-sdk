package io.dapr.it.testcontainers;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to hold workflow-related classes and their instances.
 */
public class WorkflowSingleton {
    private static WorkflowSingleton instance;
    private final List<String> payloads;
    private String workflowId;

    private WorkflowSingleton() {
        this.payloads = new ArrayList<>();
    }

    public static synchronized WorkflowSingleton getInstance() {
        if (instance == null) {
            instance = new WorkflowSingleton();
        }
        return instance;
    }

    public List<String> getPayloads() {
        return payloads;
    }

    public void addPayload(String payload) {
        payloads.add(payload);
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public void clear() {
        payloads.clear();
        workflowId = null;
    }
} 