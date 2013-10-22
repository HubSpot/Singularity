package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Resources {

  private final int cpus;
  private final int memoryMb;

  @JsonCreator
  public Resources(@JsonProperty("cpus") int cpus, @JsonProperty("mem") int memoryMb) {
    this.cpus = cpus;
    this.memoryMb = memoryMb;
  }

  public int getCpus() {
    return cpus;
  }

  public int getMemoryMb() {
    return memoryMb;
  }

}
