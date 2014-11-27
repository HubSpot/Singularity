package com.hubspot.mesos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Resources {

  private final double cpus;
  private final double memoryMb;
  private final int numPorts;

  @JsonCreator
  public Resources(@JsonProperty("cpus") double cpus, @JsonProperty("memoryMb") double memoryMb, @JsonProperty("numPorts") int numPorts) {
    this.cpus = cpus;
    this.memoryMb = memoryMb;
    this.numPorts = numPorts;
  }

  @JsonProperty
  public int getNumPorts() {
    return numPorts;
  }

  @JsonProperty
  public double getCpus() {
    return cpus;
  }

  @JsonProperty
  public double getMemoryMb() {
    return memoryMb;
  }

  @JsonIgnore
  public List<SingularityResourceRequest> getAsResourceRequestList() {
    return ImmutableList.of(new SingularityResourceRequest(SingularityResourceRequest.CPU_RESOURCE_NAME, getCpus()),
        new SingularityResourceRequest(SingularityResourceRequest.MEMORY_RESOURCE_NAME, getMemoryMb()),
        new SingularityResourceRequest(SingularityResourceRequest.PORT_COUNT_RESOURCE_NAME, getNumPorts()));
  }

  @Override
  public String toString() {
    return "Resources [cpus=" + cpus + ", memoryMb=" + memoryMb + ", numPorts=" + numPorts + "]";
  }
}
