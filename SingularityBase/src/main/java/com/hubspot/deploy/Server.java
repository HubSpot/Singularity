package com.hubspot.deploy;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class Server {
  private static final Pattern REGEX_MESOS = Pattern.compile("mesos\\.(.*)");

  private final String hostname;
  private final boolean mesos;
  private final Optional<String> clusterName;
  private final Optional<MesosServerConfig> mesosDeployConfig;

  public Server(String hostname, boolean mesos, Optional<String> clusterName, Optional<MesosServerConfig> mesosDeployConfig) {
    this.hostname = hostname;
    this.mesos = mesos;
    this.clusterName = clusterName;
    this.mesosDeployConfig = mesosDeployConfig;
  }

  @JsonCreator
  public static Server fromString(String value) {
    final Matcher mesosMatch = REGEX_MESOS.matcher(value);
    Optional<String> clusterName;

    if (mesosMatch.matches()) {
      clusterName = Optional.of(mesosMatch.group(1));
    } else {
      clusterName = Optional.absent();
    }

    return new Server(value, value.startsWith("mesos"), clusterName, Optional.<MesosServerConfig>absent());
  }

  @JsonCreator
  public Server(Map<String, MesosServerConfig> value) {
    Map.Entry<String, MesosServerConfig> entry = value.entrySet().iterator().next();

    hostname = entry.getKey();
    mesos = hostname.startsWith("mesos");

    Matcher mesosMatch = REGEX_MESOS.matcher(hostname);

    if (mesosMatch.matches()) {
      clusterName = Optional.of(mesosMatch.group(1));
    } else {
      clusterName = Optional.absent();
    }

    mesosDeployConfig = Optional.of(entry.getValue());
  }

  public String getHostname() {
    return hostname;
  }

  public boolean isMesos() {
    return mesos;
  }

  public Optional<String> getClusterName() {
    return clusterName;
  }

  public Optional<MesosServerConfig> getMesosDeployConfig() {
    return mesosDeployConfig;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("hostname", hostname)
        .add("mesos", mesos)
        .add("clusterName", clusterName)
        .add("mesosDeployConfig", mesosDeployConfig)
        .toString();
  }
}
