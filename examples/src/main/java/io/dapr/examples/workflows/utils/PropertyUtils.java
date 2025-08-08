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
