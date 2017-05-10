package com.hubspot.mesos.json;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = MesosMasterStateObject.class)
public interface MesosMasterStateObjectIF {

  String getVersion();

  @JsonProperty("git_sha")
  String getGitSha();

  @JsonProperty("git_tag")
  String getGitTag();

  @JsonProperty("build_date")
  String getBuildDate();

  @JsonProperty("build_time")
  long getBuildTime();

  @JsonProperty("build_user")
  String getBuildUser();

  @JsonProperty("start_time")
  double getStartTime();

  @JsonProperty("elected_time")
  double getElectedTime();

  String getId();

  String getPid();

  String getHostname();

  @JsonProperty("activated_slaves")
  int getActivatedSlaves();

  @JsonProperty("deactivated_slaves")
  int getDeactivatedSlaves();

  String getCluster();

  String getLeader();

  @JsonProperty("log_dir")
  String getLogDir();

  Map<String, String> getFlags();

  List<MesosMasterSlaveObject> getSlaves();

  List<MesosFrameworkObject> getFrameworks();
}
