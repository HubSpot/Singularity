package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.hubspot.mesos.json.MesosResourcesObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Optional;

@Schema(description = "Singularity's view of a Mesos agent")
public class SingularityAgent extends SingularityMachineAbstraction<SingularityAgent> {
  private final String host;
  private final String rackId;
  private final Map<String, String> attributes;
  private final Optional<MesosResourcesObject> resources;

  public SingularityAgent(
    String agentId,
    String host,
    String rackId,
    Map<String, String> attributes,
    Optional<MesosResourcesObject> resources
  ) {
    super(agentId);
    this.host = host;
    this.rackId = rackId;
    this.attributes = attributes;
    this.resources = resources;
  }

  @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularityAgent(
    String agentId,
    long firstSeenAt,
    SingularityMachineStateHistoryUpdate currentState,
    String host,
    String rackId,
    Map<String, String> attributes,
    Optional<MesosResourcesObject> resources
  ) {
    this(null, firstSeenAt, currentState, host, rackId, attributes, resources, agentId);
  }

  @JsonCreator
  public SingularityAgent(
    @JsonProperty("slaveId") String slaveId,
    @JsonProperty("firstSeenAt") long firstSeenAt,
    @JsonProperty("currentState") SingularityMachineStateHistoryUpdate currentState,
    @JsonProperty("host") String host,
    @JsonProperty("rackId") String rackId,
    @JsonProperty("attributes") Map<String, String> attributes,
    @JsonProperty("resources") Optional<MesosResourcesObject> resources,
    @JsonProperty("agentId") String agentId
  ) {
    super(MoreObjects.firstNonNull(agentId, slaveId), firstSeenAt, currentState);
    this.host = host;
    this.rackId = rackId;
    this.attributes = attributes;
    this.resources = resources;
  }

  @Override
  public SingularityAgent changeState(SingularityMachineStateHistoryUpdate newState) {
    return new SingularityAgent(
      getId(),
      getFirstSeenAt(),
      newState,
      host,
      rackId,
      attributes,
      resources
    );
  }

  @Schema(description = "agent hostname")
  public String getHost() {
    return host;
  }

  @JsonIgnore
  @Override
  public String getName() {
    return String.format("%s (%s)", getHost(), getId());
  }

  @JsonIgnore
  @Override
  public String getTypeName() {
    return "Slave";
  }

  @JsonIgnore
  public SingularityAgent withResources(MesosResourcesObject resources) {
    return new SingularityAgent(
      getId(),
      getFirstSeenAt(),
      getCurrentState(),
      host,
      rackId,
      attributes,
      Optional.of(resources)
    );
  }

  @Schema(description = "agent rack ID")
  public String getRackId() {
    return rackId;
  }

  @Schema(description = "Mesos attributes associated with this agent")
  public Map<String, String> getAttributes() {
    return attributes;
  }

  @Schema(description = "Resources available to allocate on this agent")
  public Optional<MesosResourcesObject> getResources() {
    return resources;
  }

  @Override
  public String toString() {
    return (
      "SingularitySlave{" +
      "host='" +
      host +
      '\'' +
      ", rackId='" +
      rackId +
      '\'' +
      ", attributes=" +
      attributes +
      ", resources=" +
      resources +
      "} " +
      super.toString()
    );
  }
}
