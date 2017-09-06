package com.hazelcast.swarm;


import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.hazelcast.swarm.SwarmProperties.SERVICE_NAME;
import static com.hazelcast.swarm.SwarmProperties.SWARM_SYSTEM_PREFIX;

public class SwarmDiscoveryStrategy extends AbstractDiscoveryStrategy {

    private static final String DEFAULT_SERVICE_NAME =  "hz";
    private static final int PORT = 5701;

    private final MemberLocator memberLocator;
    private final String serviceName;

    public SwarmDiscoveryStrategy(MemberLocator memberLocator, ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        this.memberLocator = memberLocator;
        this.serviceName = getOrDefault(SWARM_SYSTEM_PREFIX, SERVICE_NAME, DEFAULT_SERVICE_NAME);
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        List<DiscoveryNode> nodes = new ArrayList<>();
        List<Container> containers = memberLocator.findMemberContainers(serviceName);
        getLogger().info("Containers size: " + containers.size());

        containers.stream()
                .peek(it -> getLogger().info(it.toString()))
                .map(it -> it.getNetworkSettings().getNetworks())
                .flatMap(it -> it.values().stream())
                .peek(it -> getLogger().info(it.toString()))
                .map(ContainerNetwork::getIpAddress)
                .peek(it -> getLogger().info(it))
                .map(it -> {
                    try {
                        return InetAddress.getByName(it);
                    } catch (UnknownHostException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .peek(it -> getLogger().info(it.toString()))
                .map(it -> new SimpleDiscoveryNode(new Address(it, PORT)))
                .forEach(nodes::add);

        return nodes;
    }
}
