/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.Topic;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Dapr annotations in Spring Controllers.
 */
@Component
public class DaprBeanPostProcessor implements BeanPostProcessor {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final EmbeddedValueResolver embeddedValueResolver;

  DaprBeanPostProcessor(ConfigurableBeanFactory beanFactory) {
    embeddedValueResolver = new EmbeddedValueResolver(beanFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean == null) {
      return null;
    }

    subscribeToTopics(bean.getClass(), embeddedValueResolver);

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
  private static void subscribeToTopics(Class clazz, EmbeddedValueResolver embeddedValueResolver) {
    if (clazz == null) {
      return;
    }

    subscribeToTopics(clazz.getSuperclass(), embeddedValueResolver);
    for (Method method : clazz.getDeclaredMethods()) {
      Topic topic = method.getAnnotation(Topic.class);
      if (topic == null) {
        continue;
      }

      String route = topic.name();
      PostMapping mapping = method.getAnnotation(PostMapping.class);

      if (mapping != null && mapping.path() != null && mapping.path().length >= 1) {
        route = mapping.path()[0];
      } else if (mapping != null && mapping.value() != null && mapping.value().length >= 1) {
        route = mapping.value()[0];
      }

      String topicName = embeddedValueResolver.resolveStringValue(topic.name());
      String pubSubName = embeddedValueResolver.resolveStringValue(topic.pubsubName());
      if ((topicName != null) && (topicName.length() > 0) && pubSubName != null && pubSubName.length() > 0) {
        try {
          TypeReference<HashMap<String, String>> typeRef
                  = new TypeReference<HashMap<String, String>>() {};
          Map<String, String> metadata = MAPPER.readValue(topic.metadata(), typeRef);
          DaprRuntime.getInstance().addSubscribedTopic(pubSubName, topicName, route, metadata);
        } catch (JsonProcessingException e) {
          throw new IllegalArgumentException("Error while parsing metadata: " + e.toString());
        }
      }
    }
  }
}
