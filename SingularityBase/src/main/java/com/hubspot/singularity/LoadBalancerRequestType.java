package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public enum LoadBalancerRequestType {

  ADD, REMOVE, DEPLOY, DELETE;

  public static class LoadBalancerRequestId {

    private final String id;
    private final LoadBalancerRequestType requestType;
    private final int attemptNumber;

    @JsonCreator
    public LoadBalancerRequestId(@JsonProperty("id") String id, @JsonProperty("requestType") LoadBalancerRequestType requestType, @JsonProperty("attemptNumber") Optional<Integer> attemptNumber) {
      this.id = id;
      this.requestType = requestType;
      this.attemptNumber = attemptNumber.or(1);
    }

    @Override
    public String toString() {
      return String.format("%s-%s-%s", id, requestType, attemptNumber);
    }

    public String getId() {
      return id;
    }

    public LoadBalancerRequestType getRequestType() {
      return requestType;
    }

    public int getAttemptNumber() {
      return attemptNumber;
    }

  }

}
