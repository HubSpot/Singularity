package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;

public class SingularityLoadBalancerRequest extends BaragonRequest {

  public static SingularityLoadBalancerRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityLoadBalancerRequest.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }
  
  @JsonCreator
  public SingularityLoadBalancerRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                                        @JsonProperty("loadBalancerService") SingularityLoadBalancerService loadBalancerService,
                                        @JsonProperty("addUpstreams") List<String> addUpstreams,
                                        @JsonProperty("removeUpstreams") List<String> removeUpstreams) {
    super(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams);
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
