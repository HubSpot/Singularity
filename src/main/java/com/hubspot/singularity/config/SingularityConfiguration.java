package com.hubspot.singularity.config;

import com.codahale.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityConfiguration extends Configuration {

  @JsonProperty("mesos")
  private MesosConfiguration mesosConfiguration;
  
  @JsonProperty("zookeeper")
  private ZooKeeperConfiguration zooKeeperConfiguration;

  public MesosConfiguration getMesosConfiguration() {
    return mesosConfiguration;
  }

  public void setMesosConfiguration(MesosConfiguration mesosConfiguration) {
    this.mesosConfiguration = mesosConfiguration;
  }

  public ZooKeeperConfiguration getZooKeeperConfiguration() {
    return zooKeeperConfiguration;
  }

  public void setZooKeeperConfiguration(ZooKeeperConfiguration zooKeeperConfiguration) {
    this.zooKeeperConfiguration = zooKeeperConfiguration;
  }

}
