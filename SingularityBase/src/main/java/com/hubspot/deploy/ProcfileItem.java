package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcfileItem {
  
  private final String cmd;
  private final String schedule;
  private final Integer ports;
  private final String user;
  private final boolean daemon;
  private final Optional<MesosServerConfig> resources;

  public ProcfileItem(String cmd) {
    this(cmd, null, 0, null, true, null);
  }
  
  public ProcfileItem(String cmd, int ports) {
    this(cmd, null, ports, null, true, null);
  }
  
  @JsonCreator
  public ProcfileItem(@JsonProperty("cmd") String cmd, @JsonProperty("schedule") String schedule,
                      @JsonProperty("ports") Integer ports, @JsonProperty("user") String user,
                      @JsonProperty("daemon") Boolean daemon, @JsonProperty("resources") MesosServerConfig resources) {
    this.cmd = cmd;
    this.schedule = schedule;
    this.ports = Objects.firstNonNull(ports, 0);
    this.user = user;
    this.daemon = Objects.firstNonNull(daemon, Strings.isNullOrEmpty(schedule));
    this.resources = Optional.fromNullable(resources);
  }

  public String getCmd() {
    return cmd;
  }

  public String getSchedule() {
    return schedule;
  }

  public Integer getPorts() {
    return ports;
  }

  public String getUser() {
    return user;
  }

  public boolean getDaemon() {
    return daemon;
  }

  public Optional<MesosServerConfig> getResources() {
    return resources;
  }

  @JsonIgnore
  public DeployType getDeployType() {
    if (!Strings.isNullOrEmpty(schedule)) {
      return DeployType.CRON;
    }

    if (Strings.isNullOrEmpty(schedule) && !daemon) {
      return DeployType.TASK;
    }

    return (ports != null && ports > 0) ? DeployType.WEB : DeployType.DAEMON;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("cmd", cmd)
        .add("schedule", schedule)
        .add("ports", ports)
        .add("user", user)
        .add("daemon", daemon)
        .add("resources", resources)
        .toString();
  }
}