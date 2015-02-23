package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

public class Resources {
  public static Resources add(Resources a, Resources b) {
    checkNotNull(a, "first argument of Resources.add() is null");
    checkNotNull(b, "second argument of Resources.add() is null");

    return new Resources(a.getCpus() + b.getCpus(), a.getMemoryMb() + b.getMemoryMb(), a.getNumPorts() + b.getNumPorts());
  }

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
