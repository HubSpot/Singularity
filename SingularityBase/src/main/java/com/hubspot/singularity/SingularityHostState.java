package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityHostState {

  private final boolean master;
  private final long uptime;

  private final String driverStatus;

  private final Optional<Long> millisSinceLastOffer;

  private final int offerCacheSize;
  private final double availableCachedCpus;
  private final double availableCachedMemory;

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
      @JsonProperty("mesosConnected") boolean mesosConnected,
      @JsonProperty("offerCacheSize") int offerCacheSize,
      @JsonProperty("availableCachedCpus") double availableCachedCpus,
      @JsonProperty("availableCachedMemory") double availableCachedMemory) {
    this.master = master;
    this.uptime = uptime;
    this.driverStatus = driverStatus;
    this.millisSinceLastOffer = millisSinceLastOffer;
    this.hostAddress = hostAddress;
    this.hostname = hostname;
    this.mesosMaster = mesosMaster;
    this.mesosConnected = mesosConnected;
    this.availableCachedCpus = availableCachedCpus;
    this.availableCachedMemory = availableCachedMemory;
    this.offerCacheSize = offerCacheSize;
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

  public int getOfferCacheSize() {
    return offerCacheSize;
  }

  public double getAvailableCachedCpus() {
    return availableCachedCpus;
  }

  public double getAvailableCachedMemory() {
    return availableCachedMemory;
  }

  @Override
  public String toString() {
    return "SingularityHostState{" +
        "master=" + master +
        ", uptime=" + uptime +
        ", driverStatus='" + driverStatus + '\'' +
        ", millisSinceLastOffer=" + millisSinceLastOffer +
        ", offerCacheSize=" + offerCacheSize +
        ", availableCachedCpus=" + availableCachedCpus +
        ", availableCachedMemory=" + availableCachedMemory +
        ", hostAddress='" + hostAddress + '\'' +
        ", hostname='" + hostname + '\'' +
        ", mesosMaster='" + mesosMaster + '\'' +
        ", mesosConnected=" + mesosConnected +
        '}';
  }
}
