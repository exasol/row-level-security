package com.exasol.containers;

import java.util.Map;

import org.testcontainers.containers.Container;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;

public class HostIpProvider {
    /**
     * Get the IP of a host on which a container runs.
     *
     * @param container container for which we want to know the host IP
     * @return host IP
     */
    public static String getHostIpFromContainer(final Container<? extends Container<?>> container) {
        final NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();
        final Map<String, ContainerNetwork> networks = networkSettings.getNetworks();
        if (networks.size() == 0) {
            throw new IllegalStateException("Unable to determine host IP for \"" + container.getDockerImageName()
                    + "\" container because the docker network had no entries");
        } else {
            return networks.values().iterator().next().getGateway();
        }
    }
}