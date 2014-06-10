package com.hubspot.singularity;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.AgentResponse;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public class SingularityLoadBalancerResponse extends BaragonResponse {

  public static SingularityLoadBalancerResponse fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityLoadBalancerResponse.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

  @JsonCreator
  public SingularityLoadBalancerResponse(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                                         @JsonProperty("loadBalancerState") BaragonRequestState loadBalancerState,
                                         @JsonProperty("message") Optional<String> message,
                                         @JsonProperty("agentResponses") Optional<Map<AgentRequestType, Collection<AgentResponse>>> agentResponses) {
    super(loadBalancerRequestId, loadBalancerState, message, agentResponses);
  }

  @JsonIgnore
  public byte[] getAsBytes(ObjectMapper objectMapper) throws SingularityJsonException {
    try {
      return objectMapper.writeValueAsBytes(this);
    } catch (JsonProcessingException jpe) {
      throw new SingularityJsonException(jpe);
    }
  }
}
