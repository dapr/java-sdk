package io.dapr.spring.openfeign.targeter;

import io.dapr.feign.DaprInvokeFeignClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.openfeign.Targeter;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(Targeter.class)
public class DaprClientTargeterBeanPostProcessor implements BeanPostProcessor {

  private final DaprInvokeFeignClient daprInvokeFeignClient;

  public DaprClientTargeterBeanPostProcessor(DaprInvokeFeignClient daprInvokeFeignClient) {
    this.daprInvokeFeignClient = daprInvokeFeignClient;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof Targeter) {
      return new DaprClientTargeter(daprInvokeFeignClient, (Targeter) bean);
    }
    return bean;
  }
}
