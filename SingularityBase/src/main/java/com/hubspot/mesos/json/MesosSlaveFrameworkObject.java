package com.hubspot.mesos.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MesosSlaveFrameworkObject {
  
  private final List<MesosFrameworkObject> executors;
  
  @JsonCreator
  public MesosSlaveFrameworkObject(@JsonProperty("executors") List<MesosFrameworkObject> executors) {
    this.executors = executors;
  }
  
  public List<MesosFrameworkObject> getExecutors() {
    return executors;
  }
  
}
