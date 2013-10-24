package com.hubspot.singularity.config;

import com.codahale.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityConfiguration extends Configuration {

  @JsonProperty("mesos")
  private MesosConfiguration mesosConfiguration;
  
  @JsonProperty("zookeeper")
  private ZooKeeperConfiguration zooKeeperConfiguration;

  @JsonProperty("hostname")
  private String hostname = "127.0.0.1";

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

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
}
