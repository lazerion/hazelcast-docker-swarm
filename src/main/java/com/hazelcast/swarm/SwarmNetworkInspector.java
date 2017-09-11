package com.hazelcast.swarm;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AddressLocator;
import org.apache.commons.lang.StringUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.net.InetAddress.getLocalHost;

public class SwarmNetworkInspector implements AddressLocator, MemberLocator {

    private DockerClient dockerClient;
    private ILogger logger;
    private String hostname;

    public SwarmNetworkInspector(DockerClient dockerClient, ILogger logger) {
        this.dockerClient = dockerClient;
        this.logger = logger;
    }

    InetSocketAddress findInNetwork() {
        List<Network> network = dockerClient.listNetworksCmd().exec();
        try {
            final String containerId = getHostname();
            // todo need to filter for service name and/or overlay also
            Network.ContainerNetworkConfig config = network.stream()
                    .filter(it -> it.getName().equalsIgnoreCase("hz_backend"))
                    .map(it -> dockerClient.inspectNetworkCmd().withNetworkId(it.getId()).exec())
                    .map(Network::getContainers)
                    .flatMap(it -> it.entrySet().stream())
                    .filter(it -> it.getKey().startsWith(containerId))
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
    public List<InetSocketAddress> findMemberContainers(String serviceName) {
        // warning: does not work from worker node Docker swarm constraint.
//        try {
//            final com.spotify.docker.client.DockerClient docker = DefaultDockerClient.fromEnv().build();
//
//            return docker.listTasks(Task.Criteria.builder().serviceName("hz_hazelcast").build()).stream()
//                    .peek(it -> logger.info(it.toString()))
//                    .flatMap(it -> it.networkAttachments().stream())
//                    .peek(it -> logger.info(it.toString()))
//                    .map(it -> it.addresses().iterator().next().split("/")[0])
//                    .peek(it -> logger.info(it))
//                    .map(it -> new InetSocketAddress(it, 5701))
//                    .collect(Collectors.toList());
//
//        } catch (DockerCertificateException | InterruptedException | DockerException e) {
//            return Collections.emptyList();
//        }
        return Collections.emptyList();
    }

    private String getHostname() throws UnknownHostException {
        if (StringUtils.isBlank(hostname)) {
            hostname = getLocalHost().getHostName();
            logger.info("Hostname: " + hostname);
        }

        return hostname;
    }
}
