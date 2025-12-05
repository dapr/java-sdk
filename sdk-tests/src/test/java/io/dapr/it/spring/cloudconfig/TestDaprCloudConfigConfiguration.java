package io.dapr.it.spring.cloudconfig;

import io.dapr.spring.boot.autoconfigure.client.DaprClientAutoConfiguration;
import io.dapr.spring.boot.cloudconfig.autoconfigure.DaprCloudConfigAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DaprClientAutoConfiguration.class, DaprCloudConfigAutoConfiguration.class})
public class TestDaprCloudConfigConfiguration {
}
