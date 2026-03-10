package io.quarkiverse.dapr.langchain4j.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * CDI interceptor binding applied automatically (via Quarkus {@code AnnotationsTransformer})
 * to all {@code @Tool}-annotated methods on CDI beans.
 * <p>
 * The corresponding interceptor, {@link DaprToolCallInterceptor}, intercepts these methods
 * and, when executing inside a Dapr-backed agent workflow, routes the tool call through
 * a Dapr Workflow Activity instead of executing it directly.
 */
@InterceptorBinding
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DaprAgentToolInterceptorBinding {
}