package io.dapr.spring.openfeign.autoconfigure;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Objects;

public class FeignClientAnnoationEnabledCondition implements Condition {
  @Override
  @SuppressWarnings("null")
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    try {
      ConfigurableListableBeanFactory factory = Objects.requireNonNull(context.getBeanFactory());
      String[] beanNames = factory.getBeanNamesForAnnotation(EnableFeignClients.class);
      return beanNames != null && beanNames.length > 0;
    } catch (Exception e) {
      return false;
    }
  }
}
