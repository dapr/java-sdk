/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Dapr's Spring Boot AutoConfiguration.
 */
@Configuration
@ConditionalOnWebApplication
@ComponentScan("io.dapr.springboot")
public class DaprAutoConfiguration {
}
