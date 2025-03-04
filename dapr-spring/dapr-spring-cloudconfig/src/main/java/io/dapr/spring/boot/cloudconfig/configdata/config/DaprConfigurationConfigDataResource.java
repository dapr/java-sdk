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

package io.dapr.spring.boot.cloudconfig.configdata.config;

import org.springframework.boot.context.config.ConfigDataResource;

public class DaprConfigurationConfigDataResource extends ConfigDataResource {
    private final String storeName;
    private final String secretName;

    /**
     * Create a new non-optional {@link ConfigDataResource} instance.
     */
    public DaprConfigurationConfigDataResource(String storeName, String secretName) {
        this.storeName = storeName;
        this.secretName = secretName;
    }

    /**
     * Create a new {@link ConfigDataResource} instance.
     *
     * @param optional if the resource is optional
     * @since 2.4.6
     */
    public DaprConfigurationConfigDataResource(boolean optional, String storeName, String secretName) {
        super(optional);
        this.storeName = storeName;
        this.secretName = secretName;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getSecretName() {
        return secretName;
    }
}
