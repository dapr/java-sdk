/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.spring.boot.testcontainers.lifecycle;

import io.dapr.testcontainers.DaprContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.testcontainers.Testcontainers;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;

/**
 * Fills in the {@link DaprContainer} settings that can be derived from the Spring Boot application under test, so
 * they don't need to be set by hand.
 *
 * <p>Only the {@link DaprContainer} bean annotated with {@link ServiceConnection} is customized, as it is the sidecar
 * of the application running in the current context. Values set explicitly on the container are never overridden.
 * When the application runs an embedded web server on a known port:
 * <ul>
 *   <li>the app port defaults to {@code server.port} (8080 when not set);</li>
 *   <li>the app channel address defaults to {@code host.testcontainers.internal} and the port is exposed to the
 *   containers once the context is refreshed, so the sidecar can reach the application running on the host.</li>
 * </ul>
 *
 * <p>The app health check is not enabled by default, as the health endpoint path depends on the application
 * configuration (for example {@code server.servlet.context-path}) and a wrong path keeps daprd from starting
 * subscriptions and actors. Set it explicitly with {@link DaprContainer#withAppHealthCheckPath(String)}.
 *
 * <p>The port is only exposed after the web server has started: exposing it earlier makes the sidecar see the
 * application as listening before it is ready, so it misses the subscriptions declared by the application.
 */
public class DaprContainerSpringBootDefaultsPostProcessor
        implements BeanPostProcessor, BeanFactoryAware, ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent> {

  static final String HOST_CHANNEL_ADDRESS = "host.testcontainers.internal";

  private static final Logger LOGGER = LoggerFactory.getLogger(DaprContainerSpringBootDefaultsPostProcessor.class);
  private static final String DEFAULT_CHANNEL_ADDRESS = "localhost";
  private static final int DEFAULT_SERVER_PORT = 8080;
  private static final String[] WEB_SERVER_CONTEXT_CLASSES = {
      "org.springframework.boot.web.server.context.WebServerApplicationContext", // Spring Boot 4
      "org.springframework.boot.web.context.WebServerApplicationContext" // Spring Boot 3
  };

  private final IntConsumer hostPortExposer;
  private final Set<Integer> pendingHostPorts = new TreeSet<>();
  private ListableBeanFactory beanFactory;
  private ApplicationContext applicationContext;

  public DaprContainerSpringBootDefaultsPostProcessor() {
    this(Testcontainers::exposeHostPorts);
  }

  DaprContainerSpringBootDefaultsPostProcessor(IntConsumer hostPortExposer) {
    this.hostPortExposer = hostPortExposer;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory instanceof ListableBeanFactory listable ? listable : null;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof DaprContainer container && isServiceConnection(beanName) && isWebServerApplication()) {
      applyDefaults(container, applicationContext.getEnvironment());
    }
    return bean;
  }

  void applyDefaults(DaprContainer container, Environment environment) {
    Integer serverPort = environment.getProperty("server.port", Integer.class, DEFAULT_SERVER_PORT);
    if (serverPort <= 0) {
      LOGGER.debug("Random server port in use, skipping DaprContainer app defaults");
      return;
    }

    if (container.getAppPort() == null) {
      container.withAppPort(serverPort);
    }

    if (DEFAULT_CHANNEL_ADDRESS.equals(container.getAppChannelAddress())) {
      container.withAppChannelAddress(HOST_CHANNEL_ADDRESS);
    }

    if (HOST_CHANNEL_ADDRESS.equals(container.getAppChannelAddress())) {
      pendingHostPorts.add(container.getAppPort());
    }
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext() != applicationContext) {
      return;
    }
    for (Integer port : pendingHostPorts) {
      hostPortExposer.accept(port);
    }
    pendingHostPorts.clear();
  }

  boolean isWebServerApplication() {
    if (applicationContext == null) {
      return false;
    }
    ClassLoader classLoader = applicationContext.getClassLoader();
    for (String className : WEB_SERVER_CONTEXT_CLASSES) {
      if (ClassUtils.isPresent(className, classLoader)
          && ClassUtils.resolveClassName(className, classLoader).isInstance(applicationContext)) {
        return true;
      }
    }
    return false;
  }

  private boolean isServiceConnection(String beanName) {
    if (beanFactory == null) {
      return false;
    }
    try {
      return beanFactory.findAnnotationOnBean(beanName, ServiceConnection.class) != null;
    } catch (NoSuchBeanDefinitionException e) {
      return false;
    }
  }
}
