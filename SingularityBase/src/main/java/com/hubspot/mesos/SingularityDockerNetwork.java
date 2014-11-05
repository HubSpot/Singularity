package com.hubspot.mesos;

import org.apache.mesos.Protos;

public enum SingularityDockerNetwork {
  HOST(Protos.ContainerInfo.DockerInfo.Network.HOST),
  BRIDGE(Protos.ContainerInfo.DockerInfo.Network.BRIDGE);

  private final Protos.ContainerInfo.DockerInfo.Network networkProto;

  SingularityDockerNetwork(Protos.ContainerInfo.DockerInfo.Network networkProto) {
    this.networkProto = networkProto;
  }

  public Protos.ContainerInfo.DockerInfo.Network getNetworkProto() {
    return networkProto;
  }
}
