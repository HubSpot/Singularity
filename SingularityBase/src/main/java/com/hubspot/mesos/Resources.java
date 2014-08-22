package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

  public int getNumPorts() {
    return numPorts;
  }

  public double getCpus() {
    return cpus;
  }

  public double getMemoryMb() {
    return memoryMb;
  }

  @Override
  public String toString() {
    return "Resources [cpus=" + cpus + ", memoryMb=" + memoryMb + ", numPorts=" + numPorts + "]";
  }

}
