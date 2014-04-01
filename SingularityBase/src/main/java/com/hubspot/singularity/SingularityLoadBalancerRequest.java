package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingularityLoadBalancerRequest extends SingularityJsonObject {

  private final String loadBalancerRequestId;
  
  private final List<SingularityTask> add;
  private final List<SingularityTask> remove;
  
  public static SingularityLoadBalancerRequest fromBytes(byte[] bytes, ObjectMapper objectMapper) throws Exception {
    return objectMapper.readValue(bytes, SingularityLoadBalancerRequest.class);
  }
  
  @JsonCreator
  public SingularityLoadBalancerRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId, @JsonProperty("add") List<SingularityTask> add, @JsonProperty("remove") List<SingularityTask> remove) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.add = add;
    this.remove = remove;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public List<SingularityTask> getAdd() {
    return add;
  }

  public List<SingularityTask> getRemove() {
    return remove;
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerRequest [loadBalancerRequestId=" + loadBalancerRequestId + ", add=" + add + ", remove=" + remove + "]";
  }
  
}
