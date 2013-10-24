package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Resources {

  private final int cpus;
  private final int memoryMb;
  private final int numPorts;

  @JsonCreator
  public Resources(@JsonProperty("cpus") int cpus, @JsonProperty("memoryMb") int memoryMb, @JsonProperty("numPorts") int numPorts) {
    this.cpus = cpus;
    this.memoryMb = memoryMb;
    this.numPorts = numPorts;
  }

  public int getNumPorts() {
    return numPorts;
  }
  
  public int getCpus() {
    return cpus;
  }

  public int getMemoryMb() {
    return memoryMb;
  }

}
