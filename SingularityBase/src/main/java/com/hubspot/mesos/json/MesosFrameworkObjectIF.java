package com.hubspot.mesos.json;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosFrameworkObjectIF {
  String getName();

  String getId();

  String getPid();

  String getHostname();

  @JsonProperty("webui_url")
  String getWebuiUrl();

  String getUser();

  String getRole();

  @JsonProperty("registered_time")
  long getRegisteredTime();

  @JsonProperty("unregistered_time")
  long getUnregisteredTime();

  @JsonProperty("reregistered_time")
  long getReregisteredTime();

  boolean isActive();

  boolean isCheckpoint();

  MesosResourcesObject getResources();

  @JsonProperty("used_resources")
  MesosResourcesObject getUsedResources();

  @JsonProperty("offered_resources")
  MesosResourcesObject getOfferedResources();

  List<MesosTaskObject> getTasks();
}
