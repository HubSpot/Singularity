package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosAddress {
  private final Optional<String> hostname;
  private final Optional<String> ip;
  private final Optional<Integer> port;

  @JsonCreator
  public MesosAddress(@JsonProperty("hostname") Optional<String> hostname,
                      @JsonProperty("ip") Optional<String> ip,
                      @JsonProperty("port") Optional<Integer> port) {
    this.hostname = hostname;
    this.ip = ip;
    this.port = port;
  }

  public String getHostname() {
    return hostname.orNull();
  }

  @JsonIgnore
  public boolean hasHostname() {
    return hostname.isPresent();
  }

  public String getIp() {
    return ip.orNull();
  }

  @JsonIgnore
  public boolean hasIp() {
    return ip.isPresent();
  }

  public Integer getPort() {
    return port.orNull();
  }

  @JsonIgnore
  public boolean hasPort() {
    return port.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosAddress) {
      final MesosAddress that = (MesosAddress) obj;
      return Objects.equals(this.hostname, that.hostname) &&
          Objects.equals(this.ip, that.ip) &&
          Objects.equals(this.port, that.port);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostname, ip, port);
  }

  @Override
  public String toString() {
    return "MesosAddress{" +
        "hostname=" + hostname +
        ", ip=" + ip +
        ", port=" + port +
        '}';
  }
}
