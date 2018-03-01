package com.hubspot.mesos.json;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosMasterStateObjectIF {

  Optional<String> getVersion();

  @JsonProperty("git_sha")
  Optional<String> getGitSha();

  @JsonProperty("git_tag")
  Optional<String> getGitTag();

  @JsonProperty("build_date")
  Optional<String> getBuildDate();

  @JsonProperty("build_time")
  Optional<Long> getBuildTime();

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
  Optional<String> getLogDir();

  Map<String, String> getFlags();

  List<MesosMasterSlaveObject> getSlaves();

  List<MesosFrameworkObject> getFrameworks();
}
