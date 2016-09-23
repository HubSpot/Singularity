package com.hubspot.mesos.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosMasterStateObject {

  private final String version;
  private final String gitSha;
  private final String gitTag;
  private final String buildDate;
  private final long buildTime;
  private final String buildUser;
  private final double startTime;
  private final double electedTime;
  private final String id;
  private final String pid;
  private final String hostname;
  private final int activatedSlaves;
  private final int deactivatedSlaves;
  private final String cluster;
  private final String leader;
  private final String logDir;
  private final Map<String, String> flags;
  private final List<MesosMasterSlaveObject> slaves;
  private final List<MesosFrameworkObject> frameworks;

  @JsonCreator
  public MesosMasterStateObject(@JsonProperty("version") String version, @JsonProperty("git_sha") String gitSha, @JsonProperty("git_tag") String gitTag, @JsonProperty("build_date") String buildDate,
      @JsonProperty("build_time") long buildTime, @JsonProperty("build_user") String buildUser, @JsonProperty("start_time") double startTime, @JsonProperty("elected_time") double electedTime,
      @JsonProperty("id") String id, @JsonProperty("pid") String pid, @JsonProperty("hostname") String hostname, @JsonProperty("activated_slaves") int activatedSlaves,
      @JsonProperty("deactivated_slaves") int deactivatedSlaves, @JsonProperty("cluster") String cluster, @JsonProperty("leader") String leader, @JsonProperty("log_dir") String logDir,
      @JsonProperty("flags") Map<String, String> flags, @JsonProperty("slaves") List<MesosMasterSlaveObject> slaves, @JsonProperty("frameworks") List<MesosFrameworkObject> frameworks) {
    this.version = version;
    this.gitSha = gitSha;
    this.gitTag = gitTag;
    this.buildDate = buildDate;
    this.buildTime = buildTime;
    this.buildUser = buildUser;
    this.startTime = startTime;
    this.electedTime = electedTime;
    this.id = id;
    this.pid = pid;
    this.hostname = hostname;
    this.activatedSlaves = activatedSlaves;
    this.deactivatedSlaves = deactivatedSlaves;
    this.cluster = cluster;
    this.leader = leader;
    this.logDir = logDir;
    this.flags = flags;
    this.slaves = slaves;
    this.frameworks = frameworks;
  }

  public String getVersion() {
    return version;
  }

  public String getGitSha() {
    return gitSha;
  }

  public String getGitTag() {
    return gitTag;
  }

  public String getBuildDate() {
    return buildDate;
  }

  public long getBuildTime() {
    return buildTime;
  }

  public String getBuildUser() {
    return buildUser;
  }

  public double getStartTime() {
    return startTime;
  }

  public double getElectedTime() {
    return electedTime;
  }

  public String getId() {
    return id;
  }

  public String getPid() {
    return pid;
  }

  public String getHostname() {
    return hostname;
  }

  public int getActivatedSlaves() {
    return activatedSlaves;
  }

  public int getDeactivatedSlaves() {
    return deactivatedSlaves;
  }

  public String getCluster() {
    return cluster;
  }

  public String getLeader() {
    return leader;
  }

  public String getLogDir() {
    return logDir;
  }

  public Map<String, String> getFlags() {
    return flags;
  }

  public List<MesosMasterSlaveObject> getSlaves() {
    return slaves;
  }

  public List<MesosFrameworkObject> getFrameworks() {
    return frameworks;
  }
}
