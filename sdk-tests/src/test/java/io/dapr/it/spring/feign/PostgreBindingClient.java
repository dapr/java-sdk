package io.dapr.it.spring.feign;

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClient;

import static io.dapr.it.spring.feign.DaprFeignIT.BINDING_NAME;

@FeignClient(value = "postgres-binding", url = "http://binding." + BINDING_NAME)
@UseDaprClient
public interface PostgreBindingClient {

}
