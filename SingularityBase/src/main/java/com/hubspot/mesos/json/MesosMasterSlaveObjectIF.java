package com.hubspot.mesos.json;

import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosMasterSlaveObjectIF {
  String getId();

  String getPid();

  String getHostname();

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
