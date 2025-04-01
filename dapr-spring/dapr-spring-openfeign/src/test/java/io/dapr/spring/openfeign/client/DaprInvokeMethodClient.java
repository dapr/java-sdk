package io.dapr.spring.openfeign.client;

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "invoke-method", url = "http://method.democlient/")
@UseDaprClient
public interface DaprInvokeMethodClient {
  @GetMapping(value = "/hello", produces = "text/plain;charset=utf-8")
  String getQuery();
}
