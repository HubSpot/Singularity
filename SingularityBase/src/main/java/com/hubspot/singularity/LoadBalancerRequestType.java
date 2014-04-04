package com.hubspot.singularity;

public enum LoadBalancerRequestType {

  ADD, REMOVE;
  
  public static String getLoadBalancerRequestId(SingularityTaskId taskId, LoadBalancerRequestType requestType) {
    return String.format("%s-%s", taskId.getId(), requestType);
  }

}
