package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MesosResourcesObject {

  private final int cpus;
  private final long disk;
  private final int mem;
  private final String ports;

  @JsonCreator
  public MesosResourcesObject(@JsonProperty("cpus") int cpus, @JsonProperty("disk") long disk, @JsonProperty("mem") int mem, @JsonProperty("ports") String ports) {
    this.cpus = cpus;
    this.disk = disk;
    this.mem = mem;
    this.ports = ports;
  }

  public int getNumCpus() {
    return cpus;
  }

  public long getDiskSpace() {
    return disk;
  }

  public int getMemoryMegaBytes() {
    return mem;
  }

  public String getPorts() {
    return ports;
  }

}
