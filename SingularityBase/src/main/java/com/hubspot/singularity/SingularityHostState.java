package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityHostState {

  private final boolean master;
  private final long uptime;

  private final String driverStatus;

  private final Optional<Long> millisSinceLastOffer;

  private final String hostAddress;
  private final String hostname;

  private final String mesosMaster;
  private final boolean mesosConnected;

  @JsonCreator
  public SingularityHostState(@JsonProperty("master") boolean master,
      @JsonProperty("uptime") long uptime,
      @JsonProperty("driverStatus") String driverStatus,
      @JsonProperty("millisSinceLastOffer") Optional<Long> millisSinceLastOffer,
      @JsonProperty("hostAddress") String hostAddress,
      @JsonProperty("hostname") String hostname,
      @JsonProperty("mesosMaster") String mesosMaster,
      @JsonProperty("mesosConnected") boolean mesosConnected) {
    this.master = master;
    this.uptime = uptime;
    this.driverStatus = driverStatus;
    this.millisSinceLastOffer = millisSinceLastOffer;
    this.hostAddress = hostAddress;
    this.hostname = hostname;
    this.mesosMaster = mesosMaster;
    this.mesosConnected = mesosConnected;
  }

  public String getHostAddress() {
    return hostAddress;
  }

  public boolean isMaster() {
    return master;
  }

  public long getUptime() {
    return uptime;
  }

  public String getDriverStatus() {
    return driverStatus;
  }

  public Optional<Long> getMillisSinceLastOffer() {
    return millisSinceLastOffer;
  }

  public String getHostname() {
    return hostname;
  }

  public String getMesosMaster() {
    return mesosMaster;
  }

  public boolean isMesosConnected() {
    return mesosConnected;
  }

  @Override
  public String toString() {
    return "SingularityHostState [master=" + master + ", uptime=" + uptime + ", driverStatus=" + driverStatus + ", millisSinceLastOffer=" + millisSinceLastOffer + ", hostAddress=" + hostAddress + ", hostname=" + hostname + ", mesosMaster="
        + mesosMaster + ", mesosConnected=" + mesosConnected + "]";
  }

}
