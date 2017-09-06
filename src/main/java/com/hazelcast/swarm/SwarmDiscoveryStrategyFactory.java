package com.hazelcast.swarm;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.collect.Lists;
import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.Collection;
import java.util.Map;

public class SwarmDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

    private static final Collection<PropertyDefinition> PROPERTY_DEFINITIONS = Lists.newArrayList(
            SwarmProperties.SERVICE_NAME);

    @Override
    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        return SwarmDiscoveryStrategy.class;
    }

    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger, Map<String, Comparable> properties) {
        logger.info("creating discovery strategy.");
        return new SwarmDiscoveryStrategy(createMemberLocator(logger), logger, properties);
    }

    private MemberLocator createMemberLocator(ILogger logger) {
        return new SwarmNetworkInspector(createDockerClient(), logger);
    }

    private DockerClient createDockerClient() {
        return DockerClientBuilder.getInstance(config())
                .build();
    }

    private DefaultDockerClientConfig config() {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        return builder.build();
    }

    @Override
    public Collection<PropertyDefinition> getConfigurationProperties() {
        return PROPERTY_DEFINITIONS;
    }
}
