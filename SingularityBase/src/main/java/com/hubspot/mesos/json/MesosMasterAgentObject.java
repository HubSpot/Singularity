package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class MesosMasterAgentObject extends MesosMasterSlaveObject {

  @JsonCreator
  public MesosMasterAgentObject(
    @JsonProperty("id") String id,
    @JsonProperty("pid") String pid,
    @JsonProperty("hostname") String hostname,
    @JsonProperty("registered_time") long registeredTime,
    @JsonProperty("resources") MesosResourcesObject resources,
    @JsonProperty("attributes") Map<String, String> attributes,
    @JsonProperty("used_resources") MesosResourcesObject usedResources,
    @JsonProperty("offered_resources") MesosResourcesObject offeredResources,
    @JsonProperty("reserved_resources") MesosResourcesObject reservedResources,
    @JsonProperty("unreserved_resources") MesosResourcesObject unreservedResources,
    @JsonProperty("version") String version,
    @JsonProperty("active") boolean active
  ) {
    super(
      id,
      pid,
      hostname,
      registeredTime,
      resources,
      attributes,
      usedResources,
      offeredResources,
      reservedResources,
      unreservedResources,
      version,
      active
    );
  }
}
