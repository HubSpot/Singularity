package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class SingularityLoadBalancerRequest extends SingularityJsonObject {

  private final String loadBalancerRequestId;

  private final SingularityLoadBalancerService loadBalancerService;
  
  private final List<String> addUpstreams;
  private final List<String> removeUpstreams;
  
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
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreams = addUpstreams;
    this.removeUpstreams = removeUpstreams;
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public SingularityLoadBalancerService getLoadBalancerService() {
    return loadBalancerService;
  }

  public List<String> getAddUpstreams() {
    return addUpstreams;
  }

  public List<String> getRemoveUpstreams() {
    return removeUpstreams;
  }

  @Override
  public String toString() {
    return "SingularityLoadBalancerRequest [" +
        "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
        ", loadBalancerService=" + loadBalancerService +
        ", addUpstreams=" + addUpstreams +
        ", removeUpstreams=" + removeUpstreams +
        ']';
  }
}
