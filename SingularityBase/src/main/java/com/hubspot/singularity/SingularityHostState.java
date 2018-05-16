package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the state of a singularity scheduler instance")
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

  @Schema(description = "Address for this scheduler instance (host:port)")
  public String getHostAddress() {
    return hostAddress;
  }

  @Schema(description = "`true` if this host is currently the leading singularity instance")
  public boolean isMaster() {
    return master;
  }

  @Schema(description = "Uptime for this scheduler instance")
  public long getUptime() {
    return uptime;
  }

  @Schema(description = "Status of the mesos driver for this scheduler instance")
  public String getDriverStatus() {
    return driverStatus;
  }

  @Schema(description = "Time since the last offer was received from mesos in milliseconds")
  public Optional<Long> getMillisSinceLastOffer() {
    return millisSinceLastOffer;
  }

  @Schema(description = "hostname of this scheduler instance")
  public String getHostname() {
    return hostname;
  }

  @Schema(description = "uri of the mesos master this host is connected to")
  public String getMesosMaster() {
    return mesosMaster;
  }

  @Schema(description = "`true` if currently connected to the mesos master (should only be true on the leading instance)")
  public boolean isMesosConnected() {
    return mesosConnected;
  }

  @Schema(description = "Number of offers currently in the cache")
  public int getOfferCacheSize() {
    return offerCacheSize;
  }

  @Schema(description = "Total cpus from cached offers")
  public double getAvailableCachedCpus() {
    return availableCachedCpus;
  }

  @Schema(description = "Total memory in MB from cached offers")
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
