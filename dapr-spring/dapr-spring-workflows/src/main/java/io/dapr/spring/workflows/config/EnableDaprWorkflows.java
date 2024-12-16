package io.dapr.spring.workflows.config;


import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
@Import(DaprWorkflowsConfiguration.class)
public @interface EnableDaprWorkflows {
}
