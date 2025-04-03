package io.dapr.it.spring.feign;

import io.dapr.spring.openfeign.annotation.UseDaprClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

import static io.dapr.it.spring.feign.DaprFeignIT.BINDING_NAME;

@FeignClient(value = "postgres-binding", url = "http://binding." + BINDING_NAME)
@UseDaprClient
public interface PostgreBindingClient {

  @PostMapping("/exec")
  void exec(@RequestHeader("sql") String sql, @RequestHeader("params") List<String> params);

  @PostMapping("/exec")
  void exec(@RequestHeader("sql") String sql, @RequestHeader("params") String params);

  @PostMapping("/query")
  String query(@RequestHeader("sql") String sql, @RequestHeader("params") List<String> params);
}
