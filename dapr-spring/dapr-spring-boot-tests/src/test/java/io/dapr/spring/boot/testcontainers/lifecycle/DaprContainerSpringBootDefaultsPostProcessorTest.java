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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.List;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DaprContainerSpringBootDefaultsPostProcessorTest {

  private final RecordingPostProcessor postProcessor = new RecordingPostProcessor();

  @Test
  void appliesDefaultsWhenNothingIsSet() {
    DaprContainer container = newContainer();

    postProcessor.applyDefaults(container, new MockEnvironment());

    assertThat(container.getAppPort()).isEqualTo(8080);
    assertThat(container.getAppChannelAddress()).isEqualTo("host.testcontainers.internal");
    assertThat(container.getAppHealthCheckPath()).isNull();
    assertThat(postProcessor.refresh()).containsExactly(8080);
  }

  @Test
  void exposesHostPortOnlyOnceContextIsRefreshed() {
    postProcessor.setApplicationContext(new GenericApplicationContext());
    postProcessor.applyDefaults(newContainer(), new MockEnvironment());

    assertThat(postProcessor.exposedPorts).isEmpty();

    postProcessor.onApplicationEvent(new ContextRefreshedEvent(new GenericApplicationContext()));
    assertThat(postProcessor.exposedPorts).isEmpty();

    assertThat(postProcessor.refresh()).containsExactly(8080);
    assertThat(postProcessor.refresh()).containsExactly(8080);
  }

  @Test
  void exposesExplicitPortWhenUsingHostChannelAddress() {
    DaprContainer container = newContainer()
        .withAppPort(9090)
        .withAppChannelAddress("host.testcontainers.internal");

    postProcessor.applyDefaults(container, new MockEnvironment());

    assertThat(container.getAppPort()).isEqualTo(9090);
    assertThat(postProcessor.refresh()).containsExactly(9090);
  }

  @Test
  void usesServerPortProperty() {
    DaprContainer container = newContainer();

    postProcessor.applyDefaults(container, new MockEnvironment().withProperty("server.port", "8083"));

    assertThat(container.getAppPort()).isEqualTo(8083);
    assertThat(postProcessor.refresh()).containsExactly(8083);
  }

  @Test
  void keepsExplicitValues() {
    DaprContainer container = newContainer()
        .withAppPort(9090)
        .withAppChannelAddress("my-app")
        .withAppHealthCheckPath("/health");

    postProcessor.applyDefaults(container, new MockEnvironment().withProperty("server.port", "8083"));

    assertThat(container.getAppPort()).isEqualTo(9090);
    assertThat(container.getAppChannelAddress()).isEqualTo("my-app");
    assertThat(container.getAppHealthCheckPath()).isEqualTo("/health");
    assertThat(postProcessor.refresh()).isEmpty();
  }

  @Test
  void skipsDefaultsWithRandomServerPort() {
    DaprContainer container = newContainer();

    postProcessor.applyDefaults(container, new MockEnvironment().withProperty("server.port", "0"));

    assertThat(container.getAppPort()).isNull();
    assertThat(container.getAppChannelAddress()).isEqualTo("localhost");
    assertThat(postProcessor.refresh()).isEmpty();
  }

  @Test
  void detectsWebServerApplicationContext() {
    DaprContainerSpringBootDefaultsPostProcessor processor = new DaprContainerSpringBootDefaultsPostProcessor();
    assertThat(processor.isWebServerApplication()).isFalse();

    processor.setApplicationContext(new GenericApplicationContext());
    assertThat(processor.isWebServerApplication()).isFalse();

    WebServerApplicationContext webContext = mock(WebServerApplicationContext.class);
    when(webContext.getClassLoader()).thenReturn(getClass().getClassLoader());
    processor.setApplicationContext(webContext);
    assertThat(processor.isWebServerApplication()).isTrue();
  }

  @Test
  void customizesServiceConnectionContainerInWebServerContext() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    WebServerApplicationContext webContext = mock(WebServerApplicationContext.class);
    when(webContext.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(webContext.getEnvironment()).thenReturn(new MockEnvironment().withProperty("server.port", "8083"));

    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(beanFactory)) {
      context.register(DaprContainersConfiguration.class);
      context.refresh();

      RecordingPostProcessor processor = new RecordingPostProcessor();
      processor.setBeanFactory(beanFactory);
      processor.setApplicationContext(webContext);

      DaprContainer serviceConnection = newContainer();
      DaprContainer otherApp = newContainer();
      Object notAContainer = new Object();

      assertThat(processor.postProcessBeforeInitialization(serviceConnection, "serviceConnectionContainer"))
          .isSameAs(serviceConnection);
      assertThat(processor.postProcessBeforeInitialization(otherApp, "otherAppContainer")).isSameAs(otherApp);
      assertThat(processor.postProcessBeforeInitialization(notAContainer, "notAContainer"))
          .isSameAs(notAContainer);
      assertThat(processor.postProcessBeforeInitialization(newContainer(), "unknownBean")).isNotNull();

      assertThat(serviceConnection.getAppPort()).isEqualTo(8083);
      assertThat(otherApp.getAppPort()).isNull();
      assertThat(processor.refresh()).containsExactly(8083);
    }
  }

  @Test
  void ignoresContainersWithoutListableBeanFactory() {
    RecordingPostProcessor processor = new RecordingPostProcessor(true);
    processor.setBeanFactory(mock(BeanFactory.class));
    DaprContainer container = newContainer();

    processor.postProcessBeforeInitialization(container, "serviceConnectionContainer");

    assertThat(container.getAppPort()).isNull();
  }

  @Test
  void onlyCustomizesServiceConnectionContainerInWebServerApplication() {
    new ApplicationContextRunner()
        .withBean(RecordingPostProcessor.class, () -> new RecordingPostProcessor(true))
        .withUserConfiguration(DaprContainersConfiguration.class)
        .run(context -> {
          assertThat(context.getBean("serviceConnectionContainer", DaprContainer.class).getAppPort())
              .isEqualTo(8080);
          assertThat(context.getBean("otherAppContainer", DaprContainer.class).getAppPort()).isNull();
        });
  }

  @Test
  void doesNotCustomizeOutsideWebServerApplication() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DaprContainerSpringBootDefaultsAutoConfiguration.class))
        .withUserConfiguration(DaprContainersConfiguration.class)
        .run(context -> {
          assertThat(context).hasSingleBean(DaprContainerSpringBootDefaultsPostProcessor.class);
          assertThat(context.getBean("serviceConnectionContainer", DaprContainer.class).getAppPort()).isNull();
        });
  }

  @Test
  void autoConfigurationCanBeDisabled() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DaprContainerSpringBootDefaultsAutoConfiguration.class))
        .withPropertyValues("dapr.testcontainers.app-defaults.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(DaprContainerSpringBootDefaultsPostProcessor.class));
  }

  private static DaprContainer newContainer() {
    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG).withAppName("test-app");
  }

  @Configuration(proxyBeanMethods = false)
  static class DaprContainersConfiguration {

    @Bean
    @ServiceConnection
    DaprContainer serviceConnectionContainer() {
      return newContainer();
    }

    @Bean
    DaprContainer otherAppContainer() {
      return newContainer();
    }
  }

  static class RecordingPostProcessor extends DaprContainerSpringBootDefaultsPostProcessor {

    private final Boolean webServerApplication;
    private final List<Integer> exposedPorts;
    private ApplicationContext applicationContext;

    RecordingPostProcessor() {
      this(null);
    }

    RecordingPostProcessor(Boolean webServerApplication) {
      this(webServerApplication, new ArrayList<>());
    }

    private RecordingPostProcessor(Boolean webServerApplication, List<Integer> exposedPorts) {
      super(exposedPorts::add);
      this.webServerApplication = webServerApplication;
      this.exposedPorts = exposedPorts;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      super.setApplicationContext(applicationContext);
      this.applicationContext = applicationContext;
    }

    List<Integer> refresh() {
      if (applicationContext == null) {
        setApplicationContext(new GenericApplicationContext());
      }
      onApplicationEvent(new ContextRefreshedEvent(applicationContext));
      return exposedPorts;
    }

    @Override
    boolean isWebServerApplication() {
      return webServerApplication != null ? webServerApplication : super.isWebServerApplication();
    }
  }
}
