package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.mesos.JavaUtils;

public enum LoadBalancerRequestType {

  ADD, REMOVE, DEPLOY;

  public static class LoadBalancerRequestId {
    
    private final String id;
    private final LoadBalancerRequestType requestType;

    @JsonCreator
    public LoadBalancerRequestId(@JsonProperty("id") String id, @JsonProperty("requestType") LoadBalancerRequestType requestType) {
      this.id = id;
      this.requestType = requestType;
    }
    
    public String toString() {
      return String.format("%s-%s", id, requestType);
    }

    public static LoadBalancerRequestId fromString(String string) {
      String[] items = JavaUtils.reverseSplit(string, 2, "-");
      return new LoadBalancerRequestId(items[0], LoadBalancerRequestType.valueOf(items[1]));
    }
    
    public String getId() {
      return id;
    }

    public LoadBalancerRequestType getRequestType() {
      return requestType;
    }
    
  }
  
}
