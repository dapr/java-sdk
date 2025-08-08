/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.examples.workflows.utils;

import io.dapr.config.Properties;

import java.util.HashMap;

public class PropertyUtils {

    public static Properties getProperties(String[] args) {
        Properties properties = new Properties();
        if (args != null && args.length > 0) {
            properties = new Properties(new HashMap<>() {{
                put(Properties.GRPC_PORT, args[0]);
            }});
        }

        return properties;
    }
}
