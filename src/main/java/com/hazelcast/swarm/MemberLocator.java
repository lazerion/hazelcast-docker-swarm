package com.hazelcast.swarm;


import com.github.dockerjava.api.model.Container;

import java.util.List;

interface MemberLocator {

    List<Container> findMemberContainers(String serviceName);
}
