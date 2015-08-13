package com.hubspot.mesos;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Resources {
  public static Resources add(Resources a, Resources b) {
    checkNotNull(a, "first argument of Resources.add() is null");
    checkNotNull(b, "second argument of Resources.add() is null");

    return new Resources(a.getCpus() + b.getCpus(), a.getMemoryMb() + b.getMemoryMb(), a.getNumPorts() + b.getNumPorts());
  }

  public static final Resources EMPTY_RESOURCES = new Resources(0, 0, 0);

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Resources resources = (Resources) o;

    if (Double.compare(resources.cpus, cpus) != 0) {
      return false;
    }

    if (Double.compare(resources.memoryMb, memoryMb) != 0) {
      return false;
    }

    if (numPorts != resources.numPorts) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(cpus);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(memoryMb);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + numPorts;
    return result;
  }
}
