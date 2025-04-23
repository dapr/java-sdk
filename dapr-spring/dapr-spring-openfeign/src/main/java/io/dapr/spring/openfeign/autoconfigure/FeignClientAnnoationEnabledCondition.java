package io.dapr.spring.openfeign.autoconfigure;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class FeignClientAnnoationEnabledCondition implements Condition {
  @Override
  public boolean matches(@NotNull ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
    if (context.getBeanFactory() == null) {
      return false; // Return false if context or BeanFactory is null
    }

    String[] beanNames = context.getBeanFactory().getBeanNamesForAnnotation(EnableFeignClients.class);
    return beanNames != null && beanNames.length > 0; // Check for null and non-empty array
  }
}
