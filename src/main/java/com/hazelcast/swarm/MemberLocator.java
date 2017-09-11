package com.hazelcast.swarm;


import java.net.InetSocketAddress;
import java.util.List;

interface MemberLocator {

    List<InetSocketAddress>  findMemberContainers(String serviceName);
}
