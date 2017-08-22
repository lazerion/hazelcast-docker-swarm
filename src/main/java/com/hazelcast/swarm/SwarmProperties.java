package com.hazelcast.swarm;


import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.SimplePropertyDefinition;
import com.hazelcast.core.TypeConverter;

import static com.hazelcast.config.properties.PropertyTypeConverter.STRING;

public class SwarmProperties {

    public static final String SWARM_SYSTEM_PREFIX = "hazelcast.swarm";

    public static final PropertyDefinition SERVICE_NAME = property("service-name", STRING);

    // Prevent instantiation
    private SwarmProperties() {
    }

    private static PropertyDefinition property(String key, TypeConverter typeConverter) {
        return new SimplePropertyDefinition(key, true, typeConverter);
    }
}
