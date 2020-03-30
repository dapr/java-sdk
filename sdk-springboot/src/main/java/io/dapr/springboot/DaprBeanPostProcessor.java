/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import io.dapr.Topic;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Handles Dapr annotations in Springboot Controllers.
 */
@Component
public class DaprBeanPostProcessor implements BeanPostProcessor {

  /**
   * {@inheritDoc}
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean == null) {
      return null;
    }

    subscribeToTopics(bean.getClass());

    return bean;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  /**
   * Subscribe to topics based on {@link Topic} annotations on the given class and any of ancestor classes.
   * @param clazz Controller class where {@link Topic} is expected.
   */
  private static void subscribeToTopics(Class clazz) {
    if (clazz == null) {
      return;
    }

    subscribeToTopics(clazz.getSuperclass());
    for (Method method : clazz.getDeclaredMethods()) {
      Topic topic = method.getAnnotation(Topic.class);
      if (topic == null) {
        continue;
      }

      String topicName = topic.name();
      if ((topicName != null) && (topicName.length() > 0)) {
        DaprRuntime.getInstance().addSubscribedTopic(topicName);
      }
    }
  }
}
