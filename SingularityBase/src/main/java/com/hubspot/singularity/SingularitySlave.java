package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.json.MesosResourcesObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;

/**
 * @deprecated use {@link SingularityAgent}
 */
@Deprecated
public class SingularitySlave extends SingularityAgent {

  public SingularitySlave(
    String agentId,
    String host,
    String rackId,
    Map<String, String> attributes,
    Optional<MesosResourcesObject> resources
  ) {
    super(agentId, host, rackId, attributes, resources);
  }

  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public SingularitySlave(
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
  public SingularitySlave(
    @JsonProperty("slaveId") String slaveId,
    @JsonProperty("firstSeenAt") long firstSeenAt,
    @JsonProperty("currentState") SingularityMachineStateHistoryUpdate currentState,
    @JsonProperty("host") String host,
    @JsonProperty("rackId") String rackId,
    @JsonProperty("attributes") Map<String, String> attributes,
    @JsonProperty("resources") Optional<MesosResourcesObject> resources,
    @JsonProperty("agentId") String agentId
  ) {
    super(
      slaveId,
      firstSeenAt,
      currentState,
      host,
      rackId,
      attributes,
      resources,
      agentId
    );
  }

  public SingularitySlave(SingularityAgent agent) {
    this(
      null,
      agent.getFirstSeenAt(),
      agent.getCurrentState(),
      agent.getHost(),
      agent.getRackId(),
      agent.getAttributes(),
      agent.getResources(),
      agent.getId()
    );
  }
}
