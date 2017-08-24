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
import java.util.stream.Collectors;

import static java.net.InetAddress.getLocalHost;

public class SwarmNetworkInspector implements AddressLocator, MemberLocator{

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
            Network.ContainerNetworkConfig config = network.stream()
                    .peek(it -> logger.info(it.toString()))
                    .map(it -> dockerClient.inspectNetworkCmd().withNetworkId(it.getId()).exec())
                    .peek(it -> logger.info(it.toString()))
                    .map(Network::getContainers)
                    .peek(it -> logger.info(it.toString()))
                    .flatMap(it -> it.values().stream())
                    .peek(it -> logger.info(it.toString()))
                    .filter(it -> it.getEndpointId().startsWith(hostname))
                    .findFirst()
                    .orElse(null);

            if (config == null){
                logger.warning("unable to find container network config");
                return null;
            }
            logger.info(config.getIpv4Address());
            return new InetSocketAddress(InetAddress.getByName(config.getIpv4Address()), 0);

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
