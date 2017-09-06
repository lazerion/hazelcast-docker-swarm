package com.hazelcast.swarm;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Network;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AddressLocator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.net.InetAddress.getLocalHost;

public class SwarmNetworkInspector implements AddressLocator, MemberLocator {

    private DockerClient dockerClient;
    private ILogger logger;

    public SwarmNetworkInspector(DockerClient dockerClient, ILogger logger) {
        this.dockerClient = dockerClient;
        this.logger = logger;
    }

    InetSocketAddress findInNetwork() {
        List<Network> network = dockerClient.listNetworksCmd().exec();

        try {
            String hostname = getLocalHost().getHostName();
            logger.info("Hostname: " + hostname);

            // todo need to filter for service name and/or overlay also
            Network.ContainerNetworkConfig config = network.stream()
                    .filter(it -> it.getName().equalsIgnoreCase("hz_backend"))
                    .map(it -> dockerClient.inspectNetworkCmd().withNetworkId(it.getId()).exec())
                    .map(Network::getContainers)
                    .flatMap(it -> it.entrySet().stream())
                    .filter(it -> it.getKey().startsWith(hostname))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            if (config == null) {
                logger.warning("unable to find container network config");
                return null;
            }

            // todo refine
            logger.info(config.getIpv4Address().split("/")[0]);

            return new InetSocketAddress(InetAddress.getByName(config.getIpv4Address().split("/")[0]), 0);

        } catch (UnknownHostException e) {
            logger.warning("unknown host exception", e);
            return null;
        }
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return findInNetwork();
    }

    @Override
    public InetSocketAddress getPublicAddress() {
        return findInNetwork();
    }

    @Override
    public List<Container> findMemberContainers(String serviceName) {
        return dockerClient
                .listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .peek(it -> logger.info(it.toString()))
                .filter(it -> Arrays.stream(it.getNames()).filter(iter -> iter.contains(serviceName)).count() > 0)
                .collect(Collectors.toList());
    }
}
