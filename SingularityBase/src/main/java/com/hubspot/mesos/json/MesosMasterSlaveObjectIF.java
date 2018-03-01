package com.hubspot.mesos.json;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.MesosResourcesObject;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosMasterSlaveObjectIF {
  String getId();

  String getPid();

  String getHostname();

  @JsonProperty("registered_time")
  Optional<Long> getRegisteredTime();

  MesosResourcesObject getResources();

  Map<String, String> getAttributes();

  @JsonProperty("used_resources")
  MesosResourcesObject getUsedResources();

  @JsonProperty("offered_resources")
  MesosResourcesObject getOfferedResources();

  @JsonProperty("reserved_resources")
  MesosResourcesObject getReservedResources();

  @JsonProperty("unreserved_resources")
  MesosResourcesObject getUnreservedResources();

  String getVersion();

  boolean isActive();
}
