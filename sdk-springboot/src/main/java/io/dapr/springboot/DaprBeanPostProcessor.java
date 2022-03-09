/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.springboot;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dapr.Topic;

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
  private static void subscribeToTopics(Class<?> clazz, EmbeddedValueResolver embeddedValueResolver) {
    if (clazz == null) {
      return;
    }

    subscribeToTopics(clazz.getSuperclass(), embeddedValueResolver);
    for (Method method : clazz.getDeclaredMethods()) {
      Topic topic = method.getAnnotation(Topic.class);
      if (topic == null) {
        continue;
      }
      RequestMapping clazzRequestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
      String[] clazzLevelRoute = null;
      if(clazzRequestMapping != null) {
    	  clazzLevelRoute = clazzRequestMapping.value();
      }
      String[] postValueArray = getRouteForPost(method, topic.name());
      String topicName = embeddedValueResolver.resolveStringValue(topic.name());
      String pubSubName = embeddedValueResolver.resolveStringValue(topic.pubsubName());
      if ((topicName != null) && (topicName.length() > 0) && pubSubName != null && pubSubName.length() > 0) {
        try {
          TypeReference<HashMap<String, String>> typeRef
                  = new TypeReference<HashMap<String, String>>() {};
          Map<String, String> metadata = MAPPER.readValue(topic.metadata(), typeRef);
          
          if(postValueArray != null && postValueArray.length >= 1) {
        	  for(String postvalue: postValueArray) {
        		  if(clazzLevelRoute != null && clazzLevelRoute.length >= 1) {
        			  for(String clazzLevelValue: clazzLevelRoute) {
        				  String route = clazzLevelValue + confirmLeadingSlash(postvalue);
        				  System.out.println("route: " + route);
        				  DaprRuntime.getInstance().addSubscribedTopic(pubSubName, topicName, route, metadata);
        			  }
        		  } else {
        			  System.out.println("postValue: " + postvalue);
        			  DaprRuntime.getInstance().addSubscribedTopic(pubSubName, topicName, postvalue, metadata);
        		  }
        	  }
          }
        } catch (JsonProcessingException e) {
          throw new IllegalArgumentException("Error while parsing metadata: " + e.toString());
        }
      }
    }
  }
  
	private static String confirmLeadingSlash(String s) {
		if (s != null && s.length() >= 1) {
			if (!s.substring(0, 1).equals("/")) {
				return "/" + s;
			}
		}
		return s;
	}
  
  private static String[] getRouteForPost(Method method, String topicName) {
	  String[] postValueArray = new String[] {topicName};
	  PostMapping postMapping = method.getAnnotation(PostMapping.class);
      if (postMapping != null) {
    	  if(postMapping.path() != null && postMapping.path().length >= 1) {
    		  postValueArray = postMapping.path();
    	  } else if (postMapping.value() != null && postMapping.value().length >= 1) {
    		  postValueArray = new String[postMapping.value().length];
    		  postValueArray = postMapping.value();
          }
      } else {
    	  RequestMapping reqMapping = method.getAnnotation(RequestMapping.class);
    	  for(RequestMethod reqMethod: reqMapping.method()) {
    		  if(reqMethod == RequestMethod.POST) {
    			  if(reqMapping.path() != null && reqMapping.path().length >= 1) {
    	    		  postValueArray = new String[reqMapping.path().length];
    	    		  postValueArray = reqMapping.path();
    	    	  } else if (reqMapping.value() != null && reqMapping.value().length >= 1) {
    	    		  postValueArray = new String[reqMapping.value().length];
    	    		  postValueArray = reqMapping.value();
    	          }
    			  break;
    		  }
    	  }
      }
      return postValueArray;
  }
  
}
