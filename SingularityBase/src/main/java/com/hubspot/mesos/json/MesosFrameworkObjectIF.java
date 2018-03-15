package com.hubspot.mesos.json;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.MesosResourcesObject;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosFrameworkObjectIF {
  String getName();

  String getId();

  Optional<String> getPid();

  String getHostname();

  @JsonProperty("webui_url")
  Optional<String> getWebuiUrl();

  Optional<String> getUser();

  Optional<String> getRole();

  @JsonProperty("registered_time")
  Optional<Long> getRegisteredTime();

  @JsonProperty("unregistered_time")
  Optional<Long> getUnregisteredTime();

  @JsonProperty("reregistered_time")
  Optional<Long> getReregisteredTime();

  boolean isActive();

  boolean isCheckpoint();

  MesosResourcesObject getResources();

  @JsonProperty("used_resources")
  MesosResourcesObject getUsedResources();

  @JsonProperty("offered_resources")
  MesosResourcesObject getOfferedResources();

  List<MesosTaskObject> getTasks();
}
