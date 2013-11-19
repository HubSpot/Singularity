package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityHostState extends SingularityJsonObject {

  private final boolean master;
  private final long uptime;

  private final String driverStatus;

  private final long millisSinceLastOffer;
  
  private final String hostAddress;
  private final String hostname;
  
  @JsonCreator
  public SingularityHostState(@JsonProperty("master") boolean master, @JsonProperty("uptime") long uptime, @JsonProperty("driverStatus") String driverStatus, @JsonProperty("millisSinceLastOffer") long millisSinceLastOffer, @JsonProperty("hostAddress") String hostAddress, @JsonProperty("hostname") String hostname) {
    this.master = master;
    this.uptime = uptime;
    this.driverStatus = driverStatus;
    this.millisSinceLastOffer = millisSinceLastOffer;
    this.hostAddress = hostAddress;
    this.hostname = hostname;
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

  public long getMillisSinceLastOffer() {
    return millisSinceLastOffer;
  }

  public String getHostname() {
    return hostname;
  }

  public static SingularityHostState fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityHostState.class);
  }

  @Override
  public String toString() {
    return "SingularityHostState [master=" + master + ", uptime=" + uptime + ", driverStatus=" + driverStatus + ", millisSinceLastOffer=" + millisSinceLastOffer + ", hostAddress=" + hostAddress + ", hostname=" + hostname + "]";
  }
  
}
