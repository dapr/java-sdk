/*
 * Copyright (c) 2016-2024 Team Fangkehou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.spring.boot.cloudconfig.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for creating dapr client.
 */
@ConfigurationProperties(DaprCloudConfigProperties.PROPERTY_PREFIX)
public class DaprCloudConfigProperties {

    public static final String PROPERTY_PREFIX = "dapr.cloudconfig";

    /**
     * whether enable secret store
     */
    private Boolean enabled = true;

    /**
     * get config timeout
     */
    private Integer timeout = 2000;

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
