package io.dapr.spring.openfeign.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "demo-client", url = "http://dapr.io")
public interface DemoFeignClient {
}
