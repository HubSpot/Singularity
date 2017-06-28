package com.hubspot.mesos;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Resources {
  public static Resources add(Resources a, Resources b) {
    checkNotNull(a, "first argument of Resources.add() is null");
    checkNotNull(b, "second argument of Resources.add() is null");

    return new Resources(a.getCpus() + b.getCpus(), a.getGpus() + b.getGpus(), a.getMemoryMb() + b.getMemoryMb(), a.getNumPorts() + b.getNumPorts(), a.getDiskMb() + b.getDiskMb());
  }

  public static final Resources EMPTY_RESOURCES = new Resources(0, 0, 0, 0, 0);

  private final double cpus;
  private final int gpus;
  private final double memoryMb;
  private final int numPorts;
  private final double diskMb;

  public Resources(double cpus, int gpus, double memoryMb, int numPorts) {
    this(cpus, gpus, memoryMb, numPorts, 0);
  }

  @JsonCreator
  public Resources(@JsonProperty("cpus") double cpus, @JsonProperty("gpus") int gpus, @JsonProperty("memoryMb") double memoryMb, @JsonProperty("numPorts") int numPorts, @JsonProperty("diskMb") double diskMb) {
    this.cpus = cpus;
    this.gpus = gpus;
    this.memoryMb = memoryMb;
    this.numPorts = numPorts;
    this.diskMb = diskMb;
  }

  public int getNumPorts() {
    return numPorts;
  }

  public double getCpus() {
    return cpus;
  }
  
  public int getGpus() {
    return gpus;
  }

  public double getMemoryMb() {
    return memoryMb;
  }

  public double getDiskMb() {
    return diskMb;
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
    return Double.compare(resources.cpus, cpus) == 0 &&
    	Double.compare(resources.gpus, gpus) == 0 &&
        Double.compare(resources.memoryMb, memoryMb) == 0 &&
        numPorts == resources.numPorts &&
        Double.compare(resources.diskMb, diskMb) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cpus, gpus, memoryMb, numPorts, diskMb);
  }

  @Override
  public String toString() {
    return "Resources{" +
        "cpus=" + cpus +
        ", gpus=" + gpus +
        ", memoryMb=" + memoryMb +
        ", numPorts=" + numPorts +
        ", diskMb=" + diskMb +
        '}';
  }
}
