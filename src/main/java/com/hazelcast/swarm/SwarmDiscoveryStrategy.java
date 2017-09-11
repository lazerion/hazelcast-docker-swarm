package com.hazelcast.swarm;


import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<InetSocketAddress> members = memberLocator.findMemberContainers(serviceName);
        getLogger().info("Found members size: " + members.size());

        members.forEach(it -> nodes.add(new SimpleDiscoveryNode(new Address(it))));

        return nodes;
    }
}
