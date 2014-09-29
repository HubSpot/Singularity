package com.hubspot.singularity;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityHostState extends SingularityJsonObject {

  private final boolean master;
  private final long uptime;

  private final String driverStatus;

  private final Optional<Long> millisSinceLastOffer;

  private final String hostAddress;
  private final String hostname;

  private final String mesosMaster;

  public static SingularityHostState fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityHostState.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityHostState(@JsonProperty("master") boolean master, @JsonProperty("uptime") long uptime, @JsonProperty("driverStatus") String driverStatus, @JsonProperty("millisSinceLastOffer") Optional<Long> millisSinceLastOffer, @JsonProperty("hostAddress") String hostAddress, @JsonProperty("hostname") String hostname, @JsonProperty("mesosMaster") String mesosMaster) {
    this.master = master;
    this.uptime = uptime;
    this.driverStatus = driverStatus;
    this.millisSinceLastOffer = millisSinceLastOffer;
    this.hostAddress = hostAddress;
    this.hostname = hostname;
    this.mesosMaster = mesosMaster;
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

  @Override
  public String toString() {
    return "SingularityHostState [master=" + master + ", uptime=" + uptime + ", driverStatus=" + driverStatus + ", millisSinceLastOffer=" + millisSinceLastOffer + ", hostAddress=" + hostAddress + ", hostname=" + hostname + ", mesosMaster="
        + mesosMaster + "]";
  }

}
