package io.quarkiverse.dapr.langchain4j.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding that marks an {@code @Agent}-annotated method for
 * automatic Dapr Workflow integration.
 * <p>
 * Applied at build time by {@code DaprAgenticProcessor} to all interface methods
 * carrying the {@code @Agent} annotation. This causes {@link DaprAgentMethodInterceptor}
 * to fire when the method is called, starting an {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow}
 * so that every tool call the agent makes runs inside a Dapr Workflow Activity.
 */
@InterceptorBinding
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DaprAgentInterceptorBinding {
}
