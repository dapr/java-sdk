package io.dapr.spring.openfeign.client;

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;


@FeignClient(name = "invoke-binding", url = "http://binding.democlient/")
@UseDaprClient
public interface DaprInvokeBindingClient {
  @GetMapping(value = "/create", produces = "text/plain;charset=utf-8")
  String getQuery();
}
